package com.admin.vpnshare.shizuku

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.admin.vpnshare.MainActivity
import com.admin.vpnshare.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ShizukuNotification(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Direct Shizuku Tethering",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows active direct VPN/Internet sharing status"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun build(state: ShizukuSessionUiState, isStopping: Boolean): Notification {
        val stopIntent = Intent(context, ShizukuTetherSessionService::class.java).apply {
            action = ShizukuTetherSessionService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = when {
            isStopping -> "Stopping Direct Sharing..."
            state.status == UiStatus.CONNECTED -> "Direct VPN Sharing Active (No Proxy)"
            state.status == UiStatus.LOADING -> "Connecting Direct Sharing..."
            state.status == UiStatus.ERROR -> "Direct Sharing Error"
            else -> "Direct Sharing Ready"
        }

        val text = when {
            state.status == UiStatus.CONNECTED ->
                "Clients: ${state.clientCount} | Up: ${Traffic.format(state.traffic.up)} | Down: ${Traffic.format(state.traffic.down)}"
            state.detail.isNotEmpty() -> state.detail
            else -> "Routing Hotspot clients through VPN"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openPendingIntent)
            .setOngoing(state.status == UiStatus.CONNECTED || state.status == UiStatus.LOADING)
            .setOnlyAlertOnce(true)

        if (state.status == UiStatus.CONNECTED) {
            builder.addAction(R.drawable.ic_notification, "Stop", stopPendingIntent)
        }

        return builder.build()
    }

    fun describeLoss(problem: String?): String =
        problem ?: "Shizuku session disconnected."

    companion object {
        const val CHANNEL_ID = "netshare_shizuku_tether"
    }
}

class ShizukuTetherSessionService : Service() {
    private val scope = CoroutineScope(SupervisorJob())
    private val controller = ShizukuTetherClient()
    private val notification by lazy { ShizukuNotification(this) }
    private var isStopping = false
    private var startJob: Job? = null
    private var pollerJob: Job? = null
    private var generation = 0
    private val sessionLock = Mutex()

    override fun onCreate() {
        super.onCreate()
        controller.onSessionLost = ::handleSessionLost
        liveService = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isStopping = intent?.action == ACTION_STOP
        startForeground(
            NOTIFICATION_ID,
            notification.build(
                ShizukuSessionUiState(status = UiStatus.LOADING),
                isStopping,
            ),
        )
        when {
            isStopping -> stopSession()
            else -> startSession()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startSession() {
        if (sessionState.value.isBusy) return
        val attempt = ++generation
        sessionState.update {
            it.copy(isBusy = true, status = UiStatus.LOADING, lastError = "", detail = "Starting Shizuku tunnel...")
        }
        startJob = scope.launch {
            val outcome = sessionLock.withLock {
                if (attempt != generation) return@launch
                runCatching { controller.start(logging = true, vpnMode = VpnMode.AUTO) }
            }
            if (attempt != generation) return@launch
            outcome.exceptionOrNull()?.let { failure ->
                SessionLog.error("Start failed: ${failure.javaClass.name}: ${failure.message}")
            }
            sessionState.update { current -> current.applyOutcome(outcome) }
            publishState()
            startPoller()
        }
    }

    private fun startPoller() {
        pollerJob?.cancel()
        pollerJob = scope.launch {
            while (isActive && sessionState.value.status == UiStatus.CONNECTED) {
                delay(1500)
                val statusOutcome = runCatching { controller.status() }
                sessionState.update { current -> current.applyOutcome(statusOutcome) }
                publishState()
            }
        }
    }

    private fun stopSession() {
        val stopped = ++generation
        val abandoned = startJob
        startJob = null
        pollerJob?.cancel()
        sessionState.update { it.asStopped() }
        publishState()
        scope.launch {
            abandoned?.cancelAndJoin()
            sessionLock.withLock {
                runCatching { controller.stop() }
                    .onFailure { SessionLog.error("Teardown failed: ${it.message}") }
                controller.unbindAndStopDaemon()
            }
            if (stopped == generation) stopSelf()
        }
    }

    private fun handleSessionLost() {
        pollerJob?.cancel()
        isStopping = false
        SessionLog.error("Shizuku process died; recovering hotspot")
        sessionState.update {
            it.copy(
                isBusy = false,
                status = UiStatus.ERROR,
                interfaceName = "",
                lastError = "Session ended unexpectedly.",
            )
        }
        publishState()
        scope.launch {
            val problem = runCatching { controller.releaseOrphanedDownstream() }
                .getOrElse { failure -> "Teardown failed: ${failure.message}" }
            sessionState.update { current -> current.copy(lastError = notification.describeLoss(problem)) }
            publishState()
            stopSelf()
        }
    }

    private fun publishState() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(
            NOTIFICATION_ID,
            notification.build(sessionState.value, isStopping),
        )
    }

    override fun onDestroy() {
        controller.onSessionLost = null
        controller.unbind()
        scope.cancel()
        liveService = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 202
        const val ACTION_STOP = "com.admin.vpnshare.action.STOP_SHIZUKU_TETHER"
        private val sessionState = MutableStateFlow(ShizukuSessionUiState())
        val liveState: StateFlow<ShizukuSessionUiState> = sessionState.asStateFlow()
        private var liveService: ShizukuTetherSessionService? = null
        val isRunning: Boolean get() = liveService != null
        val isSessionUp: Boolean get() = liveState.value.status == UiStatus.CONNECTED

        fun start(context: Context) {
            val intent = Intent(context, ShizukuTetherSessionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ShizukuTetherSessionService::class.java).apply {
                action = ACTION_STOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun clearError() {
            sessionState.update { it.copy(lastError = "") }
        }
    }
}
