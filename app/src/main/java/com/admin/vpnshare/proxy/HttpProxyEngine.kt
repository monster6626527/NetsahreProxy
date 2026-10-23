package com.admin.vpnshare.proxy

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * High-performance HTTP and HTTPS CONNECT proxy engine.
 * Fully audited:
 * - Solves bindSocketToNetwork before connect()
 * - Bi-directional tunnel timeout & clean close
 * - 32768-byte high efficiency buffer
 * - Robust Basic Auth decoding with colon in password support
 * - Fast DNS caching
 * - Grouped client session tracking
 */
object HttpProxyEngine {

    private const val BUFFER_SIZE = 32768
    private const val CONNECT_TIMEOUT_MS = 10000
    private const val IDLE_TIMEOUT_MS = 30000

    suspend fun handleClient(context: Context, clientSocket: Socket) {
        val clientIp = clientSocket.inetAddress?.hostAddress ?: "unknown"
        if (ProxyManager.isIpBlocked(clientIp)) {
            ProxyManager.log("Access Control Blocked: Connection attempt from $clientIp rejected.", LogType.ERROR)
            try { clientSocket.close() } catch (e: Exception) {}
            return
        }

        ProxyManager.incrementActiveConnections()
        val session = ProxyManager.registerClientConnection(clientIp, "HTTP")

        try {
            clientSocket.soTimeout = IDLE_TIMEOUT_MS
            val input = BufferedInputStream(clientSocket.getInputStream(), BUFFER_SIZE)
            val output = clientSocket.getOutputStream()

            // Read request line
            val requestLine = ProxyManager.readLine(input) ?: return
            if (requestLine.isBlank()) return

            val parts = requestLine.trim().split("\\s+".toRegex())
            if (parts.size < 2) return

            val method = parts[0].uppercase()
            val uri = parts[1]

            // Unified headers reading
            val headers = mutableListOf<String>()
            var hostHeaderValue: String? = null
            var proxyAuthHeader: String? = null
            var userAgent: String? = null

            while (true) {
                val line = ProxyManager.readLine(input) ?: break
                if (line.isBlank()) break
                headers.add(line)
                if (line.startsWith("Host:", ignoreCase = true)) {
                    hostHeaderValue = line.substring(5).trim()
                }
                if (line.startsWith("Proxy-Authorization:", ignoreCase = true)) {
                    proxyAuthHeader = line.substring(20).trim()
                }
                if (line.startsWith("User-Agent:", ignoreCase = true)) {
                    userAgent = line.substring(11).trim()
                }
            }

            if (userAgent != null) {
                session.userAgent = userAgent
            }

            // Authentication check
            val username = ProxyManager.proxyUsername
            val password = ProxyManager.proxyPassword
            val requiresAuth = username.isNotEmpty() && password.isNotEmpty()
            if (requiresAuth) {
                var authenticated = false
                if (proxyAuthHeader != null && proxyAuthHeader.startsWith("Basic ", ignoreCase = true)) {
                    val base64Credentials = proxyAuthHeader.substring(6).trim()
                    try {
                        val decoded = String(Base64.decode(base64Credentials, Base64.NO_WRAP), Charsets.UTF_8)
                        val credParts = decoded.split(":", limit = 2)
                        if (credParts.size == 2 && credParts[0] == username && credParts[1] == password) {
                            authenticated = true
                        }
                    } catch (e: Exception) {
                        // Malformed auth
                    }
                }

                if (!authenticated) {
                    ProxyManager.log("HTTP Proxy Authentication failed from $clientIp", LogType.ERROR)
                    val response = "HTTP/1.1 407 Proxy Authentication Required\r\n" +
                            "Proxy-Authenticate: Basic realm=\"NetShare Proxy\"\r\n" +
                            "Content-Length: 35\r\n" +
                            "Connection: close\r\n\r\n" +
                            "Proxy Authentication is required.\r\n"
                    output.write(response.toByteArray(Charsets.US_ASCII))
                    output.flush()
                    return
                }
            }

            if (method == "CONNECT") {
                // HTTPS Tunneling
                val hostPort = uri.split(":")
                val host = hostPort[0]
                val port = if (hostPort.size > 1) {
                    try { hostPort[1].toInt() } catch (e: NumberFormatException) { 443 }
                } else {
                    443
                }

                ProxyManager.log("HTTPS CONNECT requested by $clientIp to $host:$port", LogType.DATA)

                try {
                    val remoteSocket = Socket()
                    ProxyManager.tuneSocket(remoteSocket)
                    // BIND TO NETWORK FIRST BEFORE CONNECTING
                    ProxyManager.bindSocketToVpn(context, remoteSocket)
                    val targetAddr = try {
                        DnsCache.resolve(host)
                    } catch (e: Exception) {
                        InetAddress.getByName(host)
                    }
                    remoteSocket.connect(InetSocketAddress(targetAddr, port), CONNECT_TIMEOUT_MS)
                    remoteSocket.soTimeout = IDLE_TIMEOUT_MS

                    output.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(Charsets.US_ASCII))
                    output.flush()

                    forwardTunnel(clientSocket, remoteSocket, customInput1 = input, session = session)
                } catch (e: Exception) {
                    try {
                        output.write("HTTP/1.1 502 Bad Gateway\r\n\r\n".toByteArray(Charsets.US_ASCII))
                        output.flush()
                    } catch (ex: Exception) {}
                    ProxyManager.log("HTTPS connection failed to $host:$port: ${e.localizedMessage}", LogType.ERROR)
                }
            } else {
                // Standard HTTP Proxying
                var host = ""
                var port = 80
                var relativePath = uri

                if (uri.startsWith("http://", ignoreCase = true)) {
                    val cleanUri = uri.substring(7)
                    val slashIdx = cleanUri.indexOf('/')
                    val hostPart = if (slashIdx != -1) cleanUri.substring(0, slashIdx) else cleanUri
                    relativePath = if (slashIdx != -1) cleanUri.substring(slashIdx) else "/"

                    if (hostPart.contains(":")) {
                        val hostPort = hostPart.split(":")
                        host = hostPort[0]
                        port = hostPort[1].toIntOrNull() ?: 80
                    } else {
                        host = hostPart
                    }
                } else if (!hostHeaderValue.isNullOrBlank()) {
                    if (hostHeaderValue.contains(":")) {
                        val hostPort = hostHeaderValue.split(":")
                        host = hostPort[0]
                        port = hostPort[1].toIntOrNull() ?: 80
                    } else {
                        host = hostHeaderValue
                    }
                }

                if (host.isNotBlank()) {
                    ProxyManager.log("HTTP $method requested by $clientIp to $host:$port$relativePath", LogType.DATA)
                    try {
                        val remoteSocket = Socket()
                        ProxyManager.tuneSocket(remoteSocket)
                        // BIND TO NETWORK FIRST BEFORE CONNECTING
                        ProxyManager.bindSocketToVpn(context, remoteSocket)
                        val targetAddr = try {
                            DnsCache.resolve(host)
                        } catch (e: Exception) {
                            InetAddress.getByName(host)
                        }
                        remoteSocket.connect(InetSocketAddress(targetAddr, port), CONNECT_TIMEOUT_MS)
                        remoteSocket.soTimeout = IDLE_TIMEOUT_MS
                        val remoteOutput = remoteSocket.getOutputStream()

                        val newRequestLine = "$method $relativePath ${parts.getOrNull(2) ?: "HTTP/1.1"}\r\n"
                        remoteOutput.write(newRequestLine.toByteArray(Charsets.US_ASCII))

                        for (header in headers) {
                            if (!header.startsWith("Proxy-Authorization:", ignoreCase = true)) {
                                remoteOutput.write("$header\r\n".toByteArray(Charsets.US_ASCII))
                            }
                        }
                        remoteOutput.write("\r\n".toByteArray(Charsets.US_ASCII))
                        remoteOutput.flush()

                        forwardTunnel(clientSocket, remoteSocket, customInput1 = input, session = session)
                    } catch (e: Exception) {
                        try {
                            output.write("HTTP/1.1 502 Bad Gateway\r\n\r\n".toByteArray(Charsets.US_ASCII))
                            output.flush()
                        } catch (ex: Exception) {}
                        ProxyManager.log("HTTP connection failed to $host:$port: ${e.localizedMessage}", LogType.ERROR)
                    }
                }
            }
        } catch (e: Exception) {
            // Closed / timeout
        } finally {
            ProxyManager.decrementActiveConnections()
            ProxyManager.unregisterClientConnection(clientIp)
            try { clientSocket.close() } catch (e: Exception) {}
        }
    }

    private suspend fun forwardTunnel(
        clientSocket: Socket,
        remoteSocket: Socket,
        customInput1: InputStream? = null,
        session: ClientSession? = null
    ) {
        ProxyManager.forwardTraffic(
            socket1 = clientSocket,
            socket2 = remoteSocket,
            customInput1 = customInput1,
            session = session
        )
    }
}
