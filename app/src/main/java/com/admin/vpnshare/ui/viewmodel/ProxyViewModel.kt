package com.admin.vpnshare.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.admin.vpnshare.proxy.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

sealed class ProxyUiState {
    object Idle : ProxyUiState()
    object Starting : ProxyUiState()
    data class Running(
        val httpPort: Int,
        val socksPort: Int,
        val bindIp: String,
        val activeConnections: Int,
        val uploadSpeedBytesPerSec: Long,
        val downloadSpeedBytesPerSec: Long,
        val totalBytesUp: Long,
        val totalBytesDown: Long
    ) : ProxyUiState()
    data class Error(val message: String) : ProxyUiState()
}

data class ProxySettings(
    val httpPort: Int = 8080,
    val socksPort: Int = 1080,
    val protocolMode: String = "BOTH", // "HTTP", "SOCKS5", "BOTH"
    val bindInterface: String = "ALL", // "WIFI", "HOTSPOT", "ALL"
    val authEnabled: Boolean = false,
    val username: String = "",
    val password: String = "",
    val startOnBoot: Boolean = false,
    val maxConnections: Int = 50,
    val pacServerEnabled: Boolean = false
)

class ProxyViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("vpn_share_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ProxySettings> = _settings.asStateFlow()

    val isHttpRunning: StateFlow<Boolean> = ProxyManager.isHttpRunning
    val isSocksRunning: StateFlow<Boolean> = ProxyManager.isSocksRunning

    val isRunning: StateFlow<Boolean> = combine(isHttpRunning, isSocksRunning) { h, s ->
        h || s
    }.stateIn(viewModelScope, SharingStarted.Eagerly, isHttpRunning.value || isSocksRunning.value)

    private val _isStarting = MutableStateFlow(false)
    val isStarting: StateFlow<Boolean> = _isStarting.asStateFlow()

    val httpPort: StateFlow<Int> = ProxyManager.httpPort
    val socksPort: StateFlow<Int> = ProxyManager.socksPort

    val logs: StateFlow<List<LogEntry>> = ProxyManager.logs
    val connectedClients: StateFlow<List<ClientSession>> = ProxyManager.connectedClients
    val activeConnections: StateFlow<Int> = ProxyManager.activeConnections
    val networkInterfaces: StateFlow<List<NetworkInterfaceInfo>> = ProxyManager.interfaces
    val blockedIps: StateFlow<Set<String>> = ProxyManager.blockedIps
    val portError: StateFlow<String?> = ProxyManager.portError
    val dataMilestoneEvent: SharedFlow<String> = ProxyManager.dataMilestoneEvent

    val usbStatus: StateFlow<UsbStatus> = USBTetherMonitor.usbStatus

    private val _selectedInterface = MutableStateFlow("ALL")
    val selectedInterface: StateFlow<String> = _selectedInterface.asStateFlow()

    private val _autoStartOnBoot = MutableStateFlow(prefs.getBoolean("start_on_boot", false))
    val autoStartOnBoot: StateFlow<Boolean> = _autoStartOnBoot.asStateFlow()

    // Real-time speed calculation (sampled every 500ms)
    private var lastUpBytes = 0L
    private var lastDownBytes = 0L
    private var lastTime = System.currentTimeMillis()

    private val _uploadSpeed = MutableStateFlow(0L)
    val uploadSpeed: StateFlow<Long> = _uploadSpeed.asStateFlow()
    val currentUpSpeed: StateFlow<Long> = _uploadSpeed.asStateFlow()

    private val _downloadSpeed = MutableStateFlow(0L)
    val downloadSpeed: StateFlow<Long> = _downloadSpeed.asStateFlow()
    val currentSpeed: StateFlow<Long> = _downloadSpeed.asStateFlow()

    // Speed History Arrays (up to 60 data points for live smooth chart)
    private val _speedHistoryDown = MutableStateFlow<List<Float>>(List(60) { 0f })
    val speedHistoryDown: StateFlow<List<Float>> = _speedHistoryDown.asStateFlow()

    private val _speedHistoryUp = MutableStateFlow<List<Float>>(List(60) { 0f })
    val speedHistoryUp: StateFlow<List<Float>> = _speedHistoryUp.asStateFlow()

    val totalBytes: StateFlow<Long> = ProxyManager.totalBytes
    val totalBytesUp: StateFlow<Long> = ProxyManager.totalBytesUp
    val totalBytesDown: StateFlow<Long> = ProxyManager.totalBytesDown

    private var sessionStartTime = System.currentTimeMillis()
    private var peakSpeed = 0L

    private val _sessionDurationFormatted = MutableStateFlow("00:00:00")
    val sessionDurationFormatted: StateFlow<String> = _sessionDurationFormatted.asStateFlow()

    private val _peakSpeedFormatted = MutableStateFlow("0.0 KB/s")
    val peakSpeedFormatted: StateFlow<String> = _peakSpeedFormatted.asStateFlow()

    private val _totalClientsServed = MutableStateFlow(0)
    val totalClientsServed: StateFlow<Int> = _totalClientsServed.asStateFlow()

    // Latency and Signal Quality
    private val _latencyMs = MutableStateFlow<Long>(24L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private val _signalBars = MutableStateFlow(4)
    val signalBars: StateFlow<Int> = _signalBars.asStateFlow()

    init {
        ProxyManager.init(application)
        applySettingsToManager(_settings.value)

        // Session timer loop
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val running = ProxyManager.isHttpRunning.value || ProxyManager.isSocksRunning.value
                if (running) {
                    val durationSeconds = (System.currentTimeMillis() - sessionStartTime) / 1000
                    val hrs = durationSeconds / 3600
                    val mins = (durationSeconds % 3600) / 60
                    val secs = durationSeconds % 60
                    _sessionDurationFormatted.value = String.format("%02d:%02d:%02d", hrs, mins, secs)
                } else {
                    sessionStartTime = System.currentTimeMillis()
                    _sessionDurationFormatted.value = "00:00:00"
                }
            }
        }

        // Fast Speed sampler loop (every 500ms)
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                val now = System.currentTimeMillis()
                val dt = ((now - lastTime) / 1000.0).coerceAtLeast(0.1)
                val currentUp = ProxyManager.totalBytesUp.value
                val currentDown = ProxyManager.totalBytesDown.value

                val downRate = (((currentDown - lastDownBytes) / dt)).toLong().coerceAtLeast(0L)
                val upRate = (((currentUp - lastUpBytes) / dt)).toLong().coerceAtLeast(0L)

                _uploadSpeed.value = upRate
                _downloadSpeed.value = downRate

                if (downRate > peakSpeed) {
                    peakSpeed = downRate
                    _peakSpeedFormatted.value = formatSpeed(peakSpeed)
                }

                // Update history points in KB/s or MB/s float
                val downKb = downRate / 1024f
                val upKb = upRate / 1024f

                _speedHistoryDown.update { history ->
                    (history.drop(1) + downKb).takeLast(60)
                }
                _speedHistoryUp.update { history ->
                    (history.drop(1) + upKb).takeLast(60)
                }

                lastUpBytes = currentUp
                lastDownBytes = currentDown
                lastTime = now
            }
        }

        // Clients served count
        viewModelScope.launch {
            val seenClientIps = mutableSetOf<String>()
            ProxyManager.connectedClients.collect { clients ->
                clients.forEach { seenClientIps.add(it.ip) }
                _totalClientsServed.value = seenClientIps.size
            }
        }

        // Connection Quality / Latency test loop
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                measureLatency()
                delay(10000L)
            }
        }
    }

    private suspend fun measureLatency() {
        withContext(Dispatchers.IO) {
            try {
                val start = System.currentTimeMillis()
                val socket = Socket()
                socket.connect(InetSocketAddress("8.8.8.8", 53), 2500)
                val latency = System.currentTimeMillis() - start
                socket.close()

                _latencyMs.value = latency
                _signalBars.value = when {
                    latency < 60 -> 4
                    latency < 120 -> 3
                    latency < 220 -> 2
                    else -> 1
                }
            } catch (e: Exception) {
                _latencyMs.value = 999L
                _signalBars.value = 1
            }
        }
    }

    private fun loadSettings(): ProxySettings {
        return ProxySettings(
            httpPort = prefs.getInt("http_port", 8080),
            socksPort = prefs.getInt("socks_port", 1080),
            protocolMode = prefs.getString("protocol_mode", "BOTH") ?: "BOTH",
            bindInterface = prefs.getString("bind_interface", "ALL") ?: "ALL",
            authEnabled = prefs.getBoolean("auth_enabled", false),
            username = prefs.getString("proxy_user", "") ?: "",
            password = prefs.getString("proxy_pass", "") ?: "",
            startOnBoot = prefs.getBoolean("start_on_boot", false),
            maxConnections = prefs.getInt("max_connections", 50),
            pacServerEnabled = prefs.getBoolean("pac_server_enabled", false)
        )
    }

    fun setSelectedInterface(iface: String) {
        _selectedInterface.value = iface
    }

    fun updateSettings(newSettings: ProxySettings) {
        _settings.value = newSettings
        prefs.edit().apply {
            putInt("http_port", newSettings.httpPort)
            putInt("socks_port", newSettings.socksPort)
            putString("protocol_mode", newSettings.protocolMode)
            putString("bind_interface", newSettings.bindInterface)
            putBoolean("auth_enabled", newSettings.authEnabled)
            putString("proxy_user", newSettings.username)
            putString("proxy_pass", newSettings.password)
            putBoolean("start_on_boot", newSettings.startOnBoot)
            putInt("max_connections", newSettings.maxConnections)
            putBoolean("pac_server_enabled", newSettings.pacServerEnabled)
            apply()
        }
        applySettingsToManager(newSettings)
    }

    fun toggleAutoStartOnBoot(sharedPrefs: android.content.SharedPreferences) {
        val next = !_autoStartOnBoot.value
        _autoStartOnBoot.value = next
        sharedPrefs.edit().putBoolean("start_on_boot", next).apply()
    }

    fun getBestIP(interfaces: List<NetworkInterfaceInfo>): String {
        // Priority 1: Hotspot/WiFi interface IP
        val hotspotOrWifi = interfaces.firstOrNull { (it.isHotspot || it.name.startsWith("wlan", ignoreCase = true)) && !it.isLoopback && it.ipAddress != "127.0.0.1" }
        if (hotspotOrWifi != null) return hotspotOrWifi.ipAddress

        // Priority 2: USB tethering interface IP
        val usb = interfaces.firstOrNull { it.isUsbTether && !it.isLoopback }
        if (usb != null) return usb.ipAddress

        // Priority 3: Any non-VPN non-loopback IP
        val nonVpn = interfaces.firstOrNull { !it.isVpn && !it.isLoopback && it.ipAddress != "127.0.0.1" }
        if (nonVpn != null) return nonVpn.ipAddress

        // Priority 4: First non-loopback IP
        val any = interfaces.firstOrNull { !it.isLoopback && it.ipAddress != "127.0.0.1" }
        if (any != null) return any.ipAddress

        return "127.0.0.1"
    }

    fun setHttpPort(port: Int) {
        ProxyManager.updateSettings(port, ProxyManager.socksPort.value)
        prefs.edit().putInt("http_port", port).apply()
    }

    fun setSocksPort(port: Int) {
        ProxyManager.updateSettings(ProxyManager.httpPort.value, port)
        prefs.edit().putInt("socks_port", port).apply()
    }

    fun startAllProxies(context: Context, hPort: Int = 8080, sPort: Int = 1080) {
        _isStarting.value = true
        ProxyManager.updateSettings(hPort, sPort)
        ProxyManager.startHttpProxy(context, hPort)
        ProxyManager.startSocksProxy(context, sPort)
        sessionStartTime = System.currentTimeMillis()
        _isStarting.value = false
    }

    fun stopAllProxies(context: Context) {
        ProxyManager.stopAll(context)
    }

    fun toggleHttpProxy(context: Context, port: Int = 8080) {
        if (ProxyManager.isHttpRunning.value) {
            ProxyManager.stopHttpProxy(context)
        } else {
            ProxyManager.startHttpProxy(context, port)
        }
    }

    fun toggleSocksProxy(context: Context, port: Int = 1080) {
        if (ProxyManager.isSocksRunning.value) {
            ProxyManager.stopSocksProxy(context)
        } else {
            ProxyManager.startSocksProxy(context, port)
        }
    }

    fun clearPortError() {
        ProxyManager.clearPortError()
    }

    fun blockIp(ip: String) {
        ProxyManager.blockIp(ip, getApplication())
    }

    fun unblockIp(ip: String) {
        ProxyManager.unblockIp(ip, getApplication())
    }

    fun clearAllBlocked() {
        ProxyManager.clearAllBlocked(getApplication())
    }

    fun clearLogs() {
        ProxyManager.clearLogs()
    }

    fun resetAllStats() {
        ProxyManager.clearStats()
        peakSpeed = 0L
        _peakSpeedFormatted.value = "0.0 KB/s"
        _speedHistoryDown.value = List(60) { 0f }
        _speedHistoryUp.value = List(60) { 0f }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        if (bytes < 1024L) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt().coerceIn(1, 6)
        val pre = "KMGTPE"[exp - 1]
        return String.format(java.util.Locale.US, "%.2f %cB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0.0 B/s"
        return when {
            bytesPerSec < 1024 -> String.format(java.util.Locale.US, "%d B/s", bytesPerSec)
            bytesPerSec < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB/s", bytesPerSec / 1024.0)
            else -> String.format(java.util.Locale.US, "%.2f MB/s", bytesPerSec / (1024.0 * 1024.0))
        }
    }

    private fun applySettingsToManager(s: ProxySettings) {
        ProxyManager.updateSettings(s.httpPort, s.socksPort)
        if (s.authEnabled && s.username.isNotBlank()) {
            ProxyManager.setCredentials(s.username, s.password)
        } else {
            ProxyManager.setCredentials("", "")
        }
    }

    // 🌟 Shizuku / Direct Tethering (Rootless Direct VPN Sharing)
    private val _shizukuState = MutableStateFlow<com.admin.vpnshare.shizuku.ShizukuState>(
        com.admin.vpnshare.shizuku.ShizukuGate.currentState()
    )
    val shizukuState: StateFlow<com.admin.vpnshare.shizuku.ShizukuState> = _shizukuState.asStateFlow()

    val shizukuSessionState: StateFlow<com.admin.vpnshare.shizuku.ShizukuSessionUiState> =
        com.admin.vpnshare.shizuku.ShizukuTetherSessionService.liveState

    private val _sharingMode = MutableStateFlow(prefs.getString("sharing_mode", "PROXY") ?: "PROXY")
    val sharingMode: StateFlow<String> = _sharingMode.asStateFlow()

    fun setSharingMode(mode: String) {
        _sharingMode.value = mode
        prefs.edit().putString("sharing_mode", mode).apply()
        refreshShizukuState()
    }

    fun refreshShizukuState() {
        _shizukuState.value = com.admin.vpnshare.shizuku.ShizukuGate.currentState()
    }

    fun requestShizukuPermission() {
        com.admin.vpnshare.shizuku.ShizukuGate.requestPermission()
        viewModelScope.launch {
            delay(1000)
            refreshShizukuState()
        }
    }

    fun remedyShizuku(context: Context) {
        com.admin.vpnshare.shizuku.ShizukuGate.remedy(context, _shizukuState.value)
    }

    fun startShizukuTether(context: Context) {
        com.admin.vpnshare.shizuku.ShizukuTetherSessionService.start(context)
    }

    fun stopShizukuTether(context: Context) {
        com.admin.vpnshare.shizuku.ShizukuTetherSessionService.stop(context)
    }

    fun clearShizukuError() {
        com.admin.vpnshare.shizuku.ShizukuTetherSessionService.clearError()
    }
}
