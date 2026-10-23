package com.admin.vpnshare.proxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.admin.vpnshare.MainActivity
import com.admin.vpnshare.R
import kotlinx.coroutines.*

class ProxyService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var updateJob: Job? = null

    private var lastBytesDown = 0L
    private var lastBytesUp = 0L
    private var lastSampleTime = System.currentTimeMillis()

    companion object {
        const val CHANNEL_ID = "netshare_proxy_service_channel"
        const val NOTIFICATION_ID = 1101
        const val ACTION_STOP = "com.admin.vpnshare.proxy.ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundWithNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ALWAYS fulfill startForeground contract first
        startForegroundWithNotification()

        if (intent?.action == ACTION_STOP) {
            try {
                ProxyManager.stopAll(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startStatsObservationLoop()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        serviceScope.cancel()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // If both HTTP and SOCKS5 are stopped, stop foreground service when task is removed
        if (!ProxyManager.isHttpRunning.value && !ProxyManager.isSocksRunning.value) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val name = "NetShare Proxy Service"
                val descriptionText = "Shows active host IP, ports, and proxy bandwidth metrics"
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                }
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getActiveHostIp(): String {
        val interfaces = ProxyManager.interfaces.value
        val hotspotOrWifi = interfaces.firstOrNull { (it.isHotspot || it.name.startsWith("wlan", ignoreCase = true)) && !it.isLoopback && it.ipAddress != "127.0.0.1" && it.ipAddress.isNotEmpty() }
        if (hotspotOrWifi != null) return hotspotOrWifi.ipAddress

        val usb = interfaces.firstOrNull { it.isUsbTether && !it.isLoopback && it.ipAddress.isNotEmpty() }
        if (usb != null) return usb.ipAddress

        val nonVpn = interfaces.firstOrNull { !it.isVpn && !it.isLoopback && it.ipAddress != "127.0.0.1" && it.ipAddress.isNotEmpty() }
        if (nonVpn != null) return nonVpn.ipAddress

        val any = interfaces.firstOrNull { !it.isLoopback && it.ipAddress != "127.0.0.1" && it.ipAddress.isNotEmpty() }
        if (any != null) return any.ipAddress

        return "127.0.0.1"
    }

    private fun startForegroundWithNotification() {
        try {
            createNotificationChannel()
            val hostIp = getActiveHostIp()
            val hPort = ProxyManager.httpPort.value
            val isHttp = ProxyManager.isHttpRunning.value
            val isSocks = ProxyManager.isSocksRunning.value
            val sPort = ProxyManager.socksPort.value

            val title = when {
                isHttp && isSocks -> "$hostIp:$hPort (SOCKS5 :$sPort)"
                isHttp -> "$hostIp:$hPort"
                isSocks -> "$hostIp:$sPort (SOCKS5)"
                else -> "$hostIp:$hPort"
            }
            val notification = buildNotification(title, "Host: $hostIp | Server Online", hostIp)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            try {
                val hostIp = getActiveHostIp()
                val fallbackNotification = buildNotification("$hostIp:${ProxyManager.httpPort.value}", "Service active", hostIp)
                startForeground(NOTIFICATION_ID, fallbackNotification)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun buildNotification(title: String, content: String, hostIp: String = ""): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, ProxyService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Stop Server", stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (hostIp.isNotEmpty() && hostIp != "127.0.0.1") {
            builder.setSubText("Host: $hostIp")
        }

        return builder.build()
    }

    private fun startStatsObservationLoop() {
        updateJob?.cancel()
        lastBytesDown = ProxyManager.totalBytesDown.value
        lastBytesUp = ProxyManager.totalBytesUp.value
        lastSampleTime = System.currentTimeMillis()

        updateJob = serviceScope.launch {
            while (isActive) {
                delay(2000L)
                val now = System.currentTimeMillis()
                val elapsedSec = ((now - lastSampleTime) / 1000.0).coerceAtLeast(0.1)

                val currentDown = ProxyManager.totalBytesDown.value
                val currentUp = ProxyManager.totalBytesUp.value
                val currentConns = ProxyManager.activeConnections.value

                val downSpeedBps = ((currentDown - lastBytesDown) / elapsedSec).coerceAtLeast(0.0)
                val upSpeedBps = ((currentUp - lastBytesUp) / elapsedSec).coerceAtLeast(0.0)

                lastBytesDown = currentDown
                lastBytesUp = currentUp
                lastSampleTime = now

                val isHttp = ProxyManager.isHttpRunning.value
                val isSocks = ProxyManager.isSocksRunning.value
                val hPort = ProxyManager.httpPort.value
                val sPort = ProxyManager.socksPort.value
                val hostIp = getActiveHostIp()

                val title = when {
                    isHttp && isSocks -> "$hostIp:$hPort (SOCKS5:$sPort)"
                    isHttp -> "$hostIp:$hPort"
                    isSocks -> "$hostIp:$sPort (SOCKS5)"
                    else -> "NetShare Proxy — $hostIp"
                }

                val downStr = formatSpeed(downSpeedBps)
                val upStr = formatSpeed(upSpeedBps)
                val clientStr = if (currentConns == 1) "1 client" else "$currentConns clients"
                val content = "↑ $upStr   ↓ $downStr  |  $clientStr"

                val notification = buildNotification(title, content, hostIp)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun formatSpeed(bytesPerSec: Double): String {
        return when {
            bytesPerSec < 1024 -> String.format("%.0f B/s", bytesPerSec)
            bytesPerSec < 1024 * 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024)
            else -> String.format("%.2f MB/s", bytesPerSec / (1024 * 1024))
        }
    }
}
