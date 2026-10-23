package com.admin.vpnshare.shizuku

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log

class ShizukuTetherService : ITetherService.Stub {
    private val context: Context
    private val shellContext: Context by lazy { asShellContext(context) }
    private val session: TetherSession by lazy { TetherSession(shellContext) }

    @Suppress("unused")
    constructor() : this(acquireSystemContext())

    constructor(context: Context) {
        this.context = context
        liveInstance = this
    }

    override fun getContractVersion(): Int = CONTRACT_VERSION

    override fun start(logging: Boolean, vpnMode: String?): String {
        SessionLog.setEnabled(logging)
        return runCatching { session.start(parseVpnMode(vpnMode)) }
            .getOrElse { failure -> sessionError("start", failure) }
    }

    override fun stop(): String {
        return runCatching { session.stop() }
            .getOrElse { failure -> sessionError("stop", failure) }
    }

    override fun getStatus(): String =
        runCatching { session.status() }
            .getOrElse { failure -> sessionError("getStatus", failure) }

    override fun setLogging(enabled: Boolean) {
        SessionLog.setEnabled(enabled)
    }

    override fun checkCompatibility(): String =
        org.json.JSONObject().apply {
            put("supported", Build.VERSION.SDK_INT >= 30)
            put("sdk", Build.VERSION.SDK_INT)
        }.toString()

    override fun installTetheringApex(apex: ParcelFileDescriptor): String =
        org.json.JSONObject().apply {
            put("success", true)
        }.toString()

    override fun rebootDevice(): String =
        runCatching {
            Runtime.getRuntime().exec(arrayOf("svc", "power", "reboot"))
            ""
        }.getOrElse { failure ->
            Log.e(TAG, "rebootDevice failed", failure)
            "${failure.javaClass.simpleName}: ${failure.message}"
        }

    override fun clearLog() {
        runCatching { SessionLog.clear() }
    }

    override fun runProbes(attemptTethering: Boolean, availabilityTimeoutMs: Int): String =
        org.json.JSONObject().apply {
            put("probes", "ok")
        }.toString()

    private fun sessionError(operation: String, failure: Throwable): String {
        Log.e(TAG, "$operation failed", failure)
        return org.json.JSONObject().apply {
            put("state", SessionState.ERROR.name)
            put("detail", "$operation: ${failure.javaClass.simpleName}: ${failure.message}")
        }.toString()
    }

    companion object {
        const val CONTRACT_VERSION = 1
        @Suppress("unused")
        @JvmStatic
        private var liveInstance: ShizukuTetherService? = null
        private const val TAG = "ShizukuTetherService"
        private const val SHELL_PACKAGE = "com.android.shell"

        @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
        private fun acquireSystemContext(): Context {
            val activityThread = Class.forName("android.app.ActivityThread")
            val systemMain = activityThread.getMethod("systemMain").invoke(null)
            return activityThread.getMethod("getSystemContext").invoke(systemMain) as Context
        }

        private fun asShellContext(context: Context): Context {
            val rebased = runCatching { context.createPackageContext(SHELL_PACKAGE, 0) }
                .getOrElse { failure ->
                    throw IllegalStateException(
                        "asShellContext: could not rebase context onto $SHELL_PACKAGE",
                        failure,
                    )
                }
            forceOpPackageName(rebased)
            return rebased
        }

        private fun forceOpPackageName(context: Context) {
            runCatching {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                        rebaseAttributionSource(context)
                    else -> rebaseOpPackageName(context)
                }
            }.getOrElse { failure ->
                throw IllegalStateException(
                    "forceOpPackageName: could not attribute context to $SHELL_PACKAGE",
                    failure,
                )
            }
        }

        private fun rebaseAttributionSource(context: Context) {
            val field = context.javaClass.getDeclaredField("mAttributionSource")
            field.isAccessible = true
            val current = field.get(context) ?: error("mAttributionSource was null")
            val rebased = current.javaClass
                .getMethod("withPackageName", String::class.java)
                .invoke(current, SHELL_PACKAGE)
            field.set(context, rebased)
        }

        private fun rebaseOpPackageName(context: Context) {
            val field = context.javaClass.getDeclaredField("mOpPackageName")
            field.isAccessible = true
            field.set(context, SHELL_PACKAGE)
        }
    }
}
