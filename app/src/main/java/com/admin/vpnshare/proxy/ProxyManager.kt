package com.admin.vpnshare.proxy

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

data class NetworkInterfaceInfo(
    val name: String,
    val ipAddress: String,
    val isVpn: Boolean,
    val isHotspot: Boolean = false,
    val isUsbTether: Boolean = false,
    val isLoopback: Boolean = false
)

enum class LogType {
    INFO, SUCCESS, WARN, ERROR, DATA
}

data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp)),
    val message: String,
    val type: LogType = LogType.INFO
)

data class ClientSession(
    val ip: String,
    val protocols: MutableSet<String> = Collections.synchronizedSet(mutableSetOf()),
    val firstConnectedAt: Long = System.currentTimeMillis(),
    val activeConnectionCount: AtomicInteger = AtomicInteger(0),
    val totalBytesUp: AtomicLong = AtomicLong(0L),
    val totalBytesDown: AtomicLong = AtomicLong(0L),
    var userAgent: String = "Unknown Client"
) {
    val totalBytes: Long get() = totalBytesUp.get() + totalBytesDown.get()
}

object DnsCache {
    private data class Entry(val address: InetAddress, val expiresAt: Long)
    private val cache = ConcurrentHashMap<String, Entry>()
    private const val TTL_MS = 90_000L

    fun resolve(host: String): InetAddress {
        cache[host]?.let { if (it.expiresAt > System.currentTimeMillis()) return it.address }
        val resolved = InetAddress.getByName(host)
        cache[host] = Entry(resolved, System.currentTimeMillis() + TTL_MS)
        return resolved
    }
}

object ProxyManager {

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val proxyDispatcher = Dispatchers.IO.limitedParallelism(512)

    private val _isHttpRunning = MutableStateFlow(false)
    val isHttpRunning: StateFlow<Boolean> = _isHttpRunning.asStateFlow()

    private val _isSocksRunning = MutableStateFlow(false)
    val isSocksRunning: StateFlow<Boolean> = _isSocksRunning.asStateFlow()

    private val _httpPort = MutableStateFlow(8080)
    val httpPort: StateFlow<Int> = _httpPort.asStateFlow()

    private val _socksPort = MutableStateFlow(1080)
    val socksPort: StateFlow<Int> = _socksPort.asStateFlow()

    private val _activeConnections = MutableStateFlow(0)
    val activeConnections: StateFlow<Int> = _activeConnections.asStateFlow()

    private val _interfaces = MutableStateFlow<List<NetworkInterfaceInfo>>(emptyList())
    val interfaces: StateFlow<List<NetworkInterfaceInfo>> = _interfaces.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _connectedClients = MutableStateFlow<List<ClientSession>>(emptyList())
    val connectedClients: StateFlow<List<ClientSession>> = _connectedClients.asStateFlow()

    private val _blockedIps = MutableStateFlow<Set<String>>(emptySet())
    val blockedIps: StateFlow<Set<String>> = _blockedIps.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            loadBlockedIps()
        }
    }

    private fun loadBlockedIps() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences("vpn_share_prefs", Context.MODE_PRIVATE)
            val saved = prefs.getStringSet("blocked_ips", emptySet()) ?: emptySet()
            _blockedIps.value = saved.toSet()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistBlockedIps(set: Set<String>) {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences("vpn_share_prefs", Context.MODE_PRIVATE)
            prefs.edit().putStringSet("blocked_ips", HashSet(set)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Port error event
    private val _portError = MutableStateFlow<String?>(null)
    val portError: StateFlow<String?> = _portError.asStateFlow()

    // Data usage milestone events (e.g. 100MB, 500MB, 1GB)
    private val _dataMilestoneEvent = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val dataMilestoneEvent: SharedFlow<String> = _dataMilestoneEvent.asSharedFlow()

    // Atomic High Performance Byte Counters
    val totalBytesCounter = AtomicLong(0L)
    val totalBytesDownCounter = AtomicLong(0L)
    val totalBytesUpCounter = AtomicLong(0L)

    private val _totalBytes = MutableStateFlow(0L)
    val totalBytes: StateFlow<Long> = _totalBytes.asStateFlow()

    private val _totalBytesDown = MutableStateFlow(0L)
    val totalBytesDown: StateFlow<Long> = _totalBytesDown.asStateFlow()

    private val _totalBytesUp = MutableStateFlow(0L)
    val totalBytesUp: StateFlow<Long> = _totalBytesUp.asStateFlow()

    private val activeConnectionsCount = AtomicInteger(0)
    private val clientSessionsMap = ConcurrentHashMap<String, ClientSession>()
    private val pendingRemoval = ConcurrentHashMap<String, Job>()

    // Sockets reference to close on shutdown
    private val httpServerSockets = mutableListOf<ServerSocket>()
    private val socksServerSockets = mutableListOf<ServerSocket>()
    private var httpJob: Job? = null
    private var socksJob: Job? = null

    // Authentication settings
    var proxyUsername: String = ""
        private set
    var proxyPassword: String = ""
        private set

    // Milestones tracking set
    private val passedMilestones = Collections.synchronizedSet(mutableSetOf<Long>())

    init {
        // Background ticker for atomic byte updates & interface refresh & data milestones
        managerScope.launch {
            while (isActive) {
                val total = totalBytesCounter.get()
                val down = totalBytesDownCounter.get()
                val up = totalBytesUpCounter.get()
                _totalBytes.value = total
                _totalBytesDown.value = down
                _totalBytesUp.value = up

                checkDataMilestones(total)

                delay(500)
            }
        }

        managerScope.launch {
            while (isActive) {
                refreshInterfaces()
                delay(3000)
            }
        }
    }

    private fun checkDataMilestones(total: Long) {
        val mb100 = 100L * 1024L * 1024L
        val mb500 = 500L * 1024L * 1024L
        val gb1 = 1024L * 1024L * 1024L

        if (total >= mb100 && !passedMilestones.contains(mb100)) {
            passedMilestones.add(mb100)
            _dataMilestoneEvent.tryEmit("Data Usage Alert: 100 MB shared in this session.")
        }
        if (total >= mb500 && !passedMilestones.contains(mb500)) {
            passedMilestones.add(mb500)
            _dataMilestoneEvent.tryEmit("Data Usage Alert: 500 MB shared in this session.")
        }
        if (total >= gb1 && !passedMilestones.contains(gb1)) {
            passedMilestones.add(gb1)
            _dataMilestoneEvent.tryEmit("Data Usage Alert: 1 GB shared in this session.")
        }
    }

    fun setCredentials(user: String, pass: String) {
        proxyUsername = user
        proxyPassword = pass
        if (user.isNotEmpty() && pass.isNotEmpty()) {
            log("Authentication Enabled: Username '$user'", LogType.INFO)
        } else {
            log("Authentication Disabled (Open Access)", LogType.INFO)
        }
    }

    fun updateSettings(hPort: Int, sPort: Int) {
        _httpPort.value = hPort
        _socksPort.value = sPort
    }

    fun clearPortError() {
        _portError.value = null
    }

    fun blockIp(ip: String, context: Context? = null) {
        if (context != null && appContext == null) {
            appContext = context.applicationContext
        }
        val updated = _blockedIps.value.toMutableSet()
        updated.add(ip)
        _blockedIps.value = updated
        persistBlockedIps(updated)
        log("IP Address $ip has been added to Blocklist.", LogType.ERROR)
        // Disconnect existing client session
        pendingRemoval.remove(ip)?.cancel()
        clientSessionsMap.remove(ip)?.let {
            _connectedClients.value = clientSessionsMap.values.toList()
        }
    }

    fun unblockIp(ip: String, context: Context? = null) {
        if (context != null && appContext == null) {
            appContext = context.applicationContext
        }
        val updated = _blockedIps.value.toMutableSet()
        updated.remove(ip)
        _blockedIps.value = updated
        persistBlockedIps(updated)
        log("IP Address $ip has been unblocked.", LogType.INFO)
    }

    fun clearAllBlocked(context: Context? = null) {
        if (context != null && appContext == null) {
            appContext = context.applicationContext
        }
        _blockedIps.value = emptySet()
        persistBlockedIps(emptySet())
        log("All blocked IP addresses have been cleared.", LogType.INFO)
    }

    fun isIpBlocked(ip: String): Boolean {
        return _blockedIps.value.contains(ip)
    }

    fun incrementActiveConnections() {
        _activeConnections.value = activeConnectionsCount.incrementAndGet()
    }

    fun decrementActiveConnections() {
        _activeConnections.value = activeConnectionsCount.decrementAndGet().coerceAtLeast(0)
    }

    fun registerClientConnection(ip: String, protocol: String): ClientSession {
        pendingRemoval.remove(ip)?.cancel() // IP reconnected fast — cancel scheduled removal
        val session = clientSessionsMap.getOrPut(ip) { ClientSession(ip = ip) }
        session.protocols.add(protocol)
        session.activeConnectionCount.incrementAndGet()
        _connectedClients.value = clientSessionsMap.values.toList()
        return session
    }

    fun unregisterClientConnection(ip: String) {
        val session = clientSessionsMap[ip] ?: return
        if (session.activeConnectionCount.decrementAndGet() <= 0) {
            // 3s grace period — avoids UI flicker if the IP reconnects immediately
            val job = managerScope.launch {
                delay(3000)
                clientSessionsMap.remove(ip)
                _connectedClients.value = clientSessionsMap.values.toList()
            }
            pendingRemoval[ip] = job
        } else {
            _connectedClients.value = clientSessionsMap.values.toList()
        }
    }

    fun addClientSession(session: ClientSession) {
        clientSessionsMap[session.ip] = session
        _connectedClients.value = clientSessionsMap.values.toList()
    }

    fun removeClientSession(ip: String) {
        pendingRemoval.remove(ip)?.cancel()
        clientSessionsMap.remove(ip)
        _connectedClients.value = clientSessionsMap.values.toList()
    }

    fun log(message: String, type: LogType = LogType.INFO) {
        val entry = LogEntry(message = message, type = type)
        val current = _logs.value.toMutableList()
        current.add(entry)
        // Cap logs to 500 lines removing oldest from HEAD
        if (current.size > 500) {
            current.removeAt(0)
        }
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun clearStats() {
        totalBytesCounter.set(0L)
        totalBytesDownCounter.set(0L)
        totalBytesUpCounter.set(0L)
        _totalBytes.value = 0L
        _totalBytesDown.value = 0L
        _totalBytesUp.value = 0L
        pendingRemoval.values.forEach { it.cancel() }
        pendingRemoval.clear()
        clientSessionsMap.clear()
        _connectedClients.value = emptyList()
        _logs.value = emptyList()
    }

    fun refreshInterfaces() {
        try {
            val list = mutableListOf<NetworkInterfaceInfo>()
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return
            for (iface in Collections.list(interfaces)) {
                if (!iface.isUp) continue
                val addresses = iface.inetAddresses ?: continue
                for (addr in Collections.list(addresses)) {
                    if (addr is java.net.Inet4Address) {
                        val hostAddr = addr.hostAddress ?: continue
                        val isLoopback = addr.isLoopbackAddress || hostAddr == "127.0.0.1"
                        val isVpn = iface.name.contains("tun", ignoreCase = true) ||
                                iface.name.contains("ppp", ignoreCase = true) ||
                                iface.name.contains("p2p", ignoreCase = true)
                        val isHotspot = iface.name.contains("ap", ignoreCase = true) ||
                                iface.name.contains("swlan", ignoreCase = true) ||
                                iface.name.contains("softap", ignoreCase = true)
                        val isUsb = iface.name.contains("rndis", ignoreCase = true) ||
                                iface.name.contains("usb", ignoreCase = true) ||
                                iface.name.contains("ncm", ignoreCase = true)

                        list.add(
                            NetworkInterfaceInfo(
                                name = iface.name,
                                ipAddress = hostAddr,
                                isVpn = isVpn,
                                isHotspot = isHotspot,
                                isUsbTether = isUsb,
                                isLoopback = isLoopback
                            )
                        )
                    }
                }
            }
            _interfaces.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun tuneSocket(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            socket.sendBufferSize = 131072
            socket.receiveBufferSize = 131072
            socket.keepAlive = true
        } catch (e: Exception) {}
    }

    @Synchronized
    fun startHttpProxy(context: Context, port: Int) {
        if (_isHttpRunning.value) return
        _httpPort.value = port
        _portError.value = null

        httpJob = managerScope.launch(proxyDispatcher) {
            try {
                log("Binding HTTP Proxy Server to port $port...", LogType.INFO)
                val socketsToListen = mutableListOf<ServerSocket>()

                val localIps = getActiveLocalIps()
                for (ip in localIps) {
                    try {
                        val ss = ServerSocket()
                        ss.reuseAddress = true
                        ss.bind(InetSocketAddress(ip, port))
                        socketsToListen.add(ss)
                        log("HTTP Proxy listening on $ip:$port", LogType.SUCCESS)
                    } catch (e: Exception) {
                        log("Could not bind HTTP on $ip:$port: ${e.localizedMessage}", LogType.ERROR)
                    }
                }

                // Also bind wildcard 0.0.0.0 as master receiver
                try {
                    val wildcard = ServerSocket()
                    wildcard.reuseAddress = true
                    wildcard.bind(InetSocketAddress(port))
                    socketsToListen.add(wildcard)
                    log("HTTP Proxy listening on 0.0.0.0:$port (Wildcard)", LogType.SUCCESS)
                } catch (e: Exception) {
                    log("Wildcard HTTP bind on $port: ${e.localizedMessage}", LogType.ERROR)
                }

                if (socketsToListen.isEmpty()) {
                    val errMsg = "HTTP Proxy failed to bind to port $port. Port may be occupied."
                    log(errMsg, LogType.ERROR)
                    _portError.value = errMsg
                    _isHttpRunning.value = false
                    return@launch
                }

                synchronized(httpServerSockets) {
                    httpServerSockets.clear()
                    httpServerSockets.addAll(socketsToListen)
                }

                _isHttpRunning.value = true
                triggerService(context)

                // Spawn listener loops
                for (serverSocket in socketsToListen) {
                    launch(proxyDispatcher) {
                        while (isActive && !serverSocket.isClosed) {
                            try {
                                val clientSocket = serverSocket.accept()
                                tuneSocket(clientSocket)
                                launch(proxyDispatcher) {
                                    handleHttpClient(context, clientSocket)
                                }
                            } catch (e: SocketException) {
                                break // Server closed
                            } catch (e: Exception) {
                                if (isActive) {
                                    log("HTTP Accept error: ${e.localizedMessage}", LogType.ERROR)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                log("HTTP Proxy service error: ${e.localizedMessage}", LogType.ERROR)
            }
        }
    }

    @Synchronized
    fun stopHttpProxy(context: Context) {
        log("Stopping HTTP Proxy...", LogType.INFO)
        stopHttpProxyInternal(context)
    }

    private fun stopHttpProxyInternal(context: Context) {
        _isHttpRunning.value = false
        synchronized(httpServerSockets) {
            for (socket in httpServerSockets) {
                try { socket.close() } catch (e: Exception) {}
            }
            httpServerSockets.clear()
        }
        httpJob?.cancel()
        httpJob = null
        log("HTTP Proxy stopped.", LogType.INFO)
        triggerService(context)
    }

    @Synchronized
    fun startSocksProxy(context: Context, port: Int) {
        if (_isSocksRunning.value) return
        _socksPort.value = port
        _portError.value = null

        socksJob = managerScope.launch(proxyDispatcher) {
            try {
                log("Binding SOCKS5 Proxy Server to port $port...", LogType.INFO)
                val socketsToListen = mutableListOf<ServerSocket>()

                val localIps = getActiveLocalIps()
                for (ip in localIps) {
                    try {
                        val ss = ServerSocket()
                        ss.reuseAddress = true
                        ss.bind(InetSocketAddress(ip, port))
                        socketsToListen.add(ss)
                        log("SOCKS5 Proxy listening on $ip:$port", LogType.SUCCESS)
                    } catch (e: Exception) {
                        log("Could not bind SOCKS5 on $ip:$port: ${e.localizedMessage}", LogType.ERROR)
                    }
                }

                // Also bind wildcard 0.0.0.0 as master receiver
                try {
                    val wildcard = ServerSocket()
                    wildcard.reuseAddress = true
                    wildcard.bind(InetSocketAddress(port))
                    socketsToListen.add(wildcard)
                    log("SOCKS5 Proxy listening on 0.0.0.0:$port (Wildcard)", LogType.SUCCESS)
                } catch (e: Exception) {
                    log("Wildcard SOCKS5 bind on $port: ${e.localizedMessage}", LogType.ERROR)
                }

                if (socketsToListen.isEmpty()) {
                    val errMsg = "SOCKS5 Proxy failed to bind to port $port. Port may be occupied."
                    log(errMsg, LogType.ERROR)
                    _portError.value = errMsg
                    _isSocksRunning.value = false
                    return@launch
                }

                synchronized(socksServerSockets) {
                    socksServerSockets.clear()
                    socksServerSockets.addAll(socketsToListen)
                }

                _isSocksRunning.value = true
                triggerService(context)

                // Spawn listener loops
                for (serverSocket in socketsToListen) {
                    launch(proxyDispatcher) {
                        while (isActive && !serverSocket.isClosed) {
                            try {
                                val clientSocket = serverSocket.accept()
                                tuneSocket(clientSocket)
                                launch(proxyDispatcher) {
                                    handleSocksClient(context, clientSocket)
                                }
                            } catch (e: SocketException) {
                                break // Server closed
                            } catch (e: Exception) {
                                if (isActive) {
                                    log("SOCKS5 Accept error: ${e.localizedMessage}", LogType.ERROR)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                log("SOCKS5 Proxy service error: ${e.localizedMessage}", LogType.ERROR)
            }
        }
    }

    @Synchronized
    fun stopAll(context: Context) {
        stopHttpProxy(context)
        stopSocksProxy(context)
    }

    @Synchronized
    fun stopSocksProxy(context: Context) {
        log("Stopping SOCKS5 Proxy...", LogType.INFO)
        stopSocksProxyInternal(context)
    }

    private fun stopSocksProxyInternal(context: Context) {
        _isSocksRunning.value = false
        synchronized(socksServerSockets) {
            for (socket in socksServerSockets) {
                try { socket.close() } catch (e: Exception) {}
            }
            socksServerSockets.clear()
        }
        socksJob?.cancel()
        socksJob = null
        log("SOCKS5 Proxy stopped.", LogType.INFO)
        triggerService(context)
    }

    private fun triggerService(context: Context) {
        try {
            val isServiceNeeded = _isHttpRunning.value || _isSocksRunning.value
            val intent = Intent(context, ProxyService::class.java)
            if (isServiceNeeded) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } else {
                try {
                    context.stopService(intent)
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {
            log("Service notification trigger error: ${e.message}", LogType.WARN)
        }
    }

    suspend fun handleHttpClient(context: Context, clientSocket: Socket) {
        HttpProxyEngine.handleClient(context, clientSocket)
    }

    suspend fun handleSocksClient(context: Context, clientSocket: Socket) {
        Socks5ProxyEngine.handleClient(context, clientSocket)
    }

    /**
     * Copy socket traffic bi-directionally with high performance
     * socket1 = clientSocket, socket2 = remoteSocket (internet)
     */
    suspend fun forwardTraffic(
        socket1: Socket,
        socket2: Socket,
        customInput1: InputStream? = null,
        customInput2: InputStream? = null,
        session: ClientSession? = null
    ) {
        coroutineScope {
            val done = CompletableDeferred<Unit>()

            // Client to Remote (UPLOAD)
            val job1 = launch(proxyDispatcher) {
                try {
                    val buffer = ByteArray(32768)
                    val input = customInput1 ?: socket1.getInputStream()
                    val output = socket2.getOutputStream()
                    while (isActive) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        if (bytesRead > 0) {
                            output.write(buffer, 0, bytesRead)
                            output.flush()
                            totalBytesCounter.addAndGet(bytesRead.toLong())
                            totalBytesUpCounter.addAndGet(bytesRead.toLong())
                            session?.totalBytesUp?.addAndGet(bytesRead.toLong())
                        }
                    }
                } catch (e: Exception) {
                    // Closed
                } finally {
                    try { socket2.close() } catch (e: Exception) {}
                    try { socket1.close() } catch (e: Exception) {}
                }
            }

            // Remote to Client (DOWNLOAD)
            val job2 = launch(proxyDispatcher) {
                try {
                    val buffer = ByteArray(32768)
                    val input = customInput2 ?: socket2.getInputStream()
                    val output = socket1.getOutputStream()
                    while (isActive) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        if (bytesRead > 0) {
                            output.write(buffer, 0, bytesRead)
                            output.flush()
                            totalBytesCounter.addAndGet(bytesRead.toLong())
                            totalBytesDownCounter.addAndGet(bytesRead.toLong())
                            session?.totalBytesDown?.addAndGet(bytesRead.toLong())
                        }
                    }
                } catch (e: Exception) {
                    // Closed
                } finally {
                    try { socket1.close() } catch (e: Exception) {}
                    try { socket2.close() } catch (e: Exception) {}
                }
            }

            job1.invokeOnCompletion { done.complete(Unit) }
            job2.invokeOnCompletion { done.complete(Unit) }

            try {
                done.await()
            } finally {
                job1.cancel()
                job2.cancel()
                try { socket1.close() } catch (e: Exception) {}
                try { socket2.close() } catch (e: Exception) {}
            }
        }
    }

    fun readLine(input: InputStream): String? {
        val out = java.io.ByteArrayOutputStream()
        var c: Int
        while (true) {
            c = input.read()
            if (c == -1) {
                if (out.size() == 0) return null
                break
            }
            if (c == '\n'.code) {
                break
            }
            if (c != '\r'.code) {
                out.write(c)
            }
        }
        return out.toString("UTF-8")
    }

    fun readFully(input: InputStream, buffer: ByteArray) {
        var bytesRead = 0
        while (bytesRead < buffer.size) {
            val result = input.read(buffer, bytesRead, buffer.size - bytesRead)
            if (result == -1) throw java.io.EOFException("Premature end of stream")
            bytesRead += result
        }
    }

    /**
     * Binds an outgoing socket directly to the system's VPN network interface if available.
     * Note: This MUST be called BEFORE socket.connect().
     */
    fun bindSocketToVpn(context: Context, socket: Socket) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworks = connectivityManager.allNetworks
            for (network in activeNetworks) {
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        network.bindSocket(socket)
                        log("Successfully bound outgoing socket to VPN network: $network", LogType.SUCCESS)
                        return
                    }
                }
            }
        } catch (e: Exception) {
            log("VPN routing note: using system default route (${e.localizedMessage})", LogType.INFO)
        }
    }

    private fun getActiveLocalIps(): List<String> {
        val ips = _interfaces.value
            .filter { !it.isVpn }
            .map { it.ipAddress }
            .filter { it.isNotEmpty() }
            .toMutableList()
        
        if (!ips.contains("127.0.0.1")) {
            ips.add("127.0.0.1")
        }
        return ips
    }
}
