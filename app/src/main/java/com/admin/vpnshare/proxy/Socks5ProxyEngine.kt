package com.admin.vpnshare.proxy

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Full SOCKS5 (RFC 1928) & Username/Password Auth (RFC 1929) proxy engine
 * Fully audited:
 * - 10s handshake timeout, 30s idle timeout
 * - UDP ASSOCIATE and BIND explicit rejection with 0x07
 * - Fast non-blocking domain resolution with DnsCache
 * - Accurate IPv4 and IPv6 handling
 * - Network interface pre-binding before connect()
 * - Grouped client session tracking
 */
object Socks5ProxyEngine {

    private const val BUFFER_SIZE = 32768
    private const val HANDSHAKE_TIMEOUT_MS = 10000
    private const val IDLE_TIMEOUT_MS = 30000
    private const val CONNECT_TIMEOUT_MS = 10000

    suspend fun handleClient(context: Context, clientSocket: Socket) {
        val clientIp = clientSocket.inetAddress?.hostAddress ?: "unknown"
        if (ProxyManager.isIpBlocked(clientIp)) {
            ProxyManager.log("Access Control Blocked: Connection attempt from $clientIp rejected.", LogType.ERROR)
            try { clientSocket.close() } catch (e: Exception) {}
            return
        }

        ProxyManager.incrementActiveConnections()
        val session = ProxyManager.registerClientConnection(clientIp, "SOCKS5")

        try {
            clientSocket.soTimeout = HANDSHAKE_TIMEOUT_MS
            val input = BufferedInputStream(clientSocket.getInputStream(), BUFFER_SIZE)
            val output = clientSocket.getOutputStream()

            // 1. SOCKS5 Method Negotiation Handshake
            val ver = input.read()
            if (ver == -1) {
                clientSocket.close()
                return
            }
            if (ver != 5) {
                ProxyManager.log("SOCKS Handshake failed from $clientIp: client used version $ver instead of 5", LogType.ERROR)
                clientSocket.close()
                return
            }

            val nmethods = input.read()
            if (nmethods < 1) {
                ProxyManager.log("SOCKS5 Handshake failed from $clientIp: 0 auth methods", LogType.ERROR)
                clientSocket.close()
                return
            }

            val methods = ByteArray(nmethods)
            ProxyManager.readFully(input, methods)

            val username = ProxyManager.proxyUsername
            val password = ProxyManager.proxyPassword
            val requiresAuth = username.isNotEmpty() && password.isNotEmpty()
            val supportsAuth = methods.contains(0x02.toByte())
            val supportsNoAuth = methods.contains(0x00.toByte())

            if (requiresAuth) {
                if (supportsAuth) {
                    output.write(byteArrayOf(0x05, 0x02))
                    output.flush()

                    val subVer = input.read()
                    if (subVer != 1) {
                        ProxyManager.log("SOCKS5 Auth failed from $clientIp: subversion $subVer", LogType.ERROR)
                        clientSocket.close()
                        return
                    }

                    val ulen = input.read()
                    if (ulen <= 0) { clientSocket.close(); return }
                    val unameBytes = ByteArray(ulen)
                    ProxyManager.readFully(input, unameBytes)
                    val reqUser = String(unameBytes, Charsets.UTF_8)

                    val plen = input.read()
                    if (plen <= 0) { clientSocket.close(); return }
                    val passBytes = ByteArray(plen)
                    ProxyManager.readFully(input, passBytes)
                    val reqPass = String(passBytes, Charsets.UTF_8)

                    if (reqUser == username && reqPass == password) {
                        output.write(byteArrayOf(0x01, 0x00))
                        output.flush()
                        ProxyManager.log("SOCKS5 Auth Succeeded for user '$reqUser' from $clientIp", LogType.INFO)
                    } else {
                        output.write(byteArrayOf(0x01, 0x01))
                        output.flush()
                        ProxyManager.log("SOCKS5 Auth Failed for user '$reqUser' from $clientIp", LogType.ERROR)
                        clientSocket.close()
                        return
                    }
                } else {
                    ProxyManager.log("SOCKS5 rejected $clientIp: client unsupported auth method", LogType.ERROR)
                    output.write(byteArrayOf(0x05, 0xFF.toByte()))
                    output.flush()
                    clientSocket.close()
                    return
                }
            } else {
                if (supportsNoAuth) {
                    output.write(byteArrayOf(0x05, 0x00))
                    output.flush()
                } else {
                    output.write(byteArrayOf(0x05, 0xFF.toByte()))
                    output.flush()
                    clientSocket.close()
                    return
                }
            }

            // 2. SOCKS5 Request
            val ver2 = input.read()
            if (ver2 != 5) {
                ProxyManager.log("SOCKS5 Request failed from $clientIp: sub-version $ver2", LogType.ERROR)
                return
            }

            val cmd = input.read()
            val rsv = input.read()
            val atyp = input.read()

            // 1 = CONNECT, 2 = BIND, 3 = UDP ASSOCIATE
            if (cmd != 1) {
                ProxyManager.log("SOCKS5 Request from $clientIp: Command $cmd not supported", LogType.ERROR)
                // 0x07 = Command not supported
                output.write(byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                output.flush()
                return
            }

            var targetHost = ""
            when (atyp) {
                1 -> { // IPv4 (4 bytes)
                    val ipBytes = ByteArray(4)
                    ProxyManager.readFully(input, ipBytes)
                    targetHost = InetAddress.getByAddress(ipBytes).hostAddress ?: ""
                }
                3 -> { // Domain name (1 byte length + string)
                    val len = input.read()
                    if (len > 0) {
                        val domainBytes = ByteArray(len)
                        ProxyManager.readFully(input, domainBytes)
                        targetHost = String(domainBytes, Charsets.UTF_8)
                    }
                }
                4 -> { // IPv6 (16 bytes)
                    val ipBytes = ByteArray(16)
                    ProxyManager.readFully(input, ipBytes)
                    targetHost = InetAddress.getByAddress(ipBytes).hostAddress ?: ""
                }
                else -> {
                    ProxyManager.log("SOCKS5 Request failed from $clientIp: Address type $atyp not supported", LogType.ERROR)
                    output.write(byteArrayOf(0x05, 0x08, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                    output.flush()
                    return
                }
            }

            val portByte1 = input.read()
            val portByte2 = input.read()
            val targetPort = ((portByte1 and 0xFF) shl 8) or (portByte2 and 0xFF)

            ProxyManager.log("SOCKS5 CONNECT requested by $clientIp to $targetHost:$targetPort", LogType.DATA)

            try {
                val remoteSocket = Socket()
                ProxyManager.tuneSocket(remoteSocket)
                // BIND TO NETWORK BEFORE CONNECTING
                ProxyManager.bindSocketToVpn(context, remoteSocket)
                
                withContext(Dispatchers.IO) {
                    val targetAddr = try {
                        DnsCache.resolve(targetHost)
                    } catch (e: Exception) {
                        InetAddress.getByName(targetHost)
                    }
                    remoteSocket.connect(InetSocketAddress(targetAddr, targetPort), CONNECT_TIMEOUT_MS)
                }
                
                clientSocket.soTimeout = IDLE_TIMEOUT_MS
                remoteSocket.soTimeout = IDLE_TIMEOUT_MS

                val localAddr = remoteSocket.localAddress ?: clientSocket.localAddress
                val localPort = remoteSocket.localPort
                val addrBytes = localAddr.address
                val portBytes = byteArrayOf(
                    ((localPort shr 8) and 0xFF).toByte(),
                    (localPort and 0xFF).toByte()
                )
                val reply = ByteArray(4 + addrBytes.size + 2)
                reply[0] = 0x05.toByte()
                reply[1] = 0x00.toByte() // Success
                reply[2] = 0x00.toByte() // RSV
                reply[3] = if (addrBytes.size == 4) 0x01.toByte() else 0x04.toByte()
                System.arraycopy(addrBytes, 0, reply, 4, addrBytes.size)
                System.arraycopy(portBytes, 0, reply, 4 + addrBytes.size, 2)
                output.write(reply)
                output.flush()

                ProxyManager.forwardTraffic(clientSocket, remoteSocket, customInput1 = input, session = session)
            } catch (e: Exception) {
                ProxyManager.log("SOCKS5 connection failed to $targetHost:$targetPort: ${e.localizedMessage}", LogType.ERROR)
                try {
                    // 0x04 = Host unreachable
                    output.write(byteArrayOf(0x05, 0x04, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                    output.flush()
                } catch (ex: Exception) {}
            }
        } catch (e: Exception) {
            // Closed / timeout
        } finally {
            ProxyManager.decrementActiveConnections()
            ProxyManager.unregisterClientConnection(clientIp)
            try { clientSocket.close() } catch (e: Exception) {}
        }
    }
}
