package com.admin.vpnshare.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.admin.vpnshare.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import rikka.shizuku.Shizuku

enum class UiStatus { READY, LOADING, CONNECTED, ERROR }

data class ShizukuSessionUiState(
    val shizukuState: ShizukuState = ShizukuState.NotRunning,
    val status: UiStatus = UiStatus.READY,
    val isBusy: Boolean = false,
    val detail: String = "",
    val interfaceName: String = "",
    val lastError: String = "",
    val isVpnBound: Boolean = false,
    val isVpnBypassed: Boolean = false,
    val clientCount: Int = 0,
    val traffic: Traffic = Traffic(),
) {
    val canStart: Boolean get() = shizukuState is ShizukuState.Ready && !isBusy
}

fun ShizukuSessionUiState.asStopped(): ShizukuSessionUiState = copy(
    isBusy = false,
    status = UiStatus.READY,
    lastError = "",
    detail = "Stopped",
    interfaceName = "",
    isVpnBound = false,
    isVpnBypassed = false,
    clientCount = 0,
    traffic = Traffic(),
)

fun ShizukuSessionUiState.applyOutcome(outcome: Result<String>): ShizukuSessionUiState {
    val failure = outcome.exceptionOrNull()
    if (failure != null) {
        return copy(
            isBusy = false,
            status = UiStatus.ERROR,
            lastError = "${failure.javaClass.simpleName}: ${failure.message}",
            interfaceName = "",
            isVpnBound = false,
            isVpnBypassed = false,
            clientCount = 0,
            traffic = Traffic(),
        )
    }
    val parsed = runCatching { JSONObject(outcome.getOrDefault("{}")) }.getOrNull()
    val sessionState = parsed?.optString("state").orEmpty()
    val sessionDetail = parsed?.optString("detail").orEmpty()
    return copy(
        isBusy = false,
        status = statusFor(sessionState),
        detail = sessionDetail,
        interfaceName = parsed?.optString("interface").orEmpty().takeIf { it != "null" }.orEmpty(),
        lastError = if (sessionState == "ERROR") sessionDetail else "",
        isVpnBound = parsed?.optBoolean("isVpnBound") == true,
        isVpnBypassed = parsed?.optBoolean("isVpnBypassed") == true,
        clientCount = parsed?.optInt("clientCount") ?: 0,
        traffic = Traffic(
            up = parsed?.optLong("bytesUp") ?: 0,
            down = parsed?.optLong("bytesDown") ?: 0,
        ),
    )
}

private fun statusFor(sessionState: String): UiStatus = when (sessionState) {
    "ACTIVE" -> UiStatus.CONNECTED
    "ERROR" -> UiStatus.ERROR
    "STARTING" -> UiStatus.LOADING
    else -> UiStatus.READY
}

class ShizukuTetherClient {
    private var boundService: ITetherService? = null
    private var pendingBind: CompletableDeferred<ITetherService>? = null
    var onSessionLost: (() -> Unit)? = null
    private var deathRecipient: IBinder.DeathRecipient? = null

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, ShizukuTetherService::class.java.name),
    )
        .daemon(true)
        .tag(SERVICE_TAG)
        .processNameSuffix("tether")
        .debuggable(BuildConfig.DEBUG)
        .version(ShizukuTetherService.CONTRACT_VERSION)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = binder?.let { ITetherService.Stub.asInterface(it) }
            when (service) {
                null -> pendingBind?.completeExceptionally(
                    IllegalStateException("Shizuku returned a null binder"),
                )
                else -> {
                    boundService = service
                    pendingBind?.complete(service)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }
    }

    private suspend fun service(): ITetherService {
        boundService?.let { existing ->
            if (existing.asBinder().pingBinder()) return existing
            boundService = null
        }
        val deferred = CompletableDeferred<ITetherService>()
        pendingBind = deferred
        Shizuku.bindUserService(userServiceArgs, connection)
        val bound = awaitBind(deferred)
        observeDeath(bound)
        return bound
    }

    private suspend fun awaitBind(deferred: CompletableDeferred<ITetherService>): ITetherService =
        try {
            withTimeout(BIND_TIMEOUT_MS) { deferred.await() }
        } catch (timeout: TimeoutCancellationException) {
            pendingBind = null
            throw IllegalStateException(
                "Shizuku service bind timed out. Ensure Shizuku is running and permission is granted.",
                timeout,
            )
        }

    private fun observeDeath(bound: ITetherService) {
        deathRecipient = null
        val binder = bound.asBinder()
        val recipient = IBinder.DeathRecipient {
            boundService = null
            onSessionLost?.invoke()
        }
        runCatching { binder.linkToDeath(recipient, 0) }
            .onSuccess { deathRecipient = recipient }
            .onFailure {
                boundService = null
            }
    }

    suspend fun start(logging: Boolean, vpnMode: VpnMode): String = withContext(Dispatchers.IO) {
        val bound = service()
        verifyContract(bound)
        bound.start(logging, vpnMode.name)
    }

    fun setLogging(enabled: Boolean) {
        runCatching { boundService?.setLogging(enabled) }
    }

    suspend fun stop(): String = withContext(Dispatchers.IO) {
        service().stop()
    }

    suspend fun clearLog(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bound = service()
            verifyContract(bound)
            bound.clearLog()
        }.fold(
            onSuccess = { null },
            onFailure = { failure -> failure.message ?: "Could not reach Shizuku" },
        )
    }

    suspend fun releaseOrphanedDownstream(): String? = withContext(Dispatchers.IO) {
        runCatching { service().stop() }
            .fold(
                onSuccess = { null },
                onFailure = { failure ->
                    "Could not reach Shizuku to drop hotspot: ${failure.message}"
                },
            )
    }

    suspend fun status(): String = withContext(Dispatchers.IO) {
        service().status
    }

    private fun verifyContract(bound: ITetherService) {
        val remote = bound.contractVersion
        check(remote == ShizukuTetherService.CONTRACT_VERSION) {
            "UserService is stale (app: ${ShizukuTetherService.CONTRACT_VERSION}, service: $remote). Please restart."
        }
    }

    fun unbind() = releaseBinding(shouldTerminate = false)
    fun unbindAndStopDaemon() = releaseBinding(shouldTerminate = true)

    private fun releaseBinding(shouldTerminate: Boolean) {
        deathRecipient?.let { recipient ->
            runCatching { boundService?.asBinder()?.unlinkToDeath(recipient, 0) }
        }
        deathRecipient = null
        runCatching { Shizuku.unbindUserService(userServiceArgs, connection, shouldTerminate) }
        boundService = null
    }

    private companion object {
        private const val SERVICE_TAG = "netshare-shizuku-tether"
        const val BIND_TIMEOUT_MS = 30_000L
    }
}
