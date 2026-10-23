package com.admin.vpnshare.shizuku

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.BufferedReader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val TETHER_ERROR_NAMES = mapOf(
    0 to "NO_ERROR",
    1 to "UNKNOWN_IFACE",
    2 to "SERVICE_UNAVAIL",
    3 to "UNSUPPORTED",
    4 to "UNAVAIL_IFACE",
    5 to "INTERNAL_ERROR",
    6 to "TETHER_IFACE_ERROR",
    7 to "UNTETHER_IFACE_ERROR",
    8 to "ENABLE_FORWARDING_ERROR",
    9 to "DISABLE_FORWARDING_ERROR",
    10 to "IFACE_CFG_ERROR",
    11 to "PROVISIONING_FAILED",
    12 to "DHCPSERVER_ERROR",
    13 to "ENTITLEMENT_UNKNOWN",
    14 to "NO_CHANGE_TETHERING_PERMISSION",
    15 to "NO_ACCESS_TETHERING_PERMISSION",
    16 to "UNKNOWN_TYPE",
    17 to "UNKNOWN_REQUEST",
    18 to "DUPLICATE_REQUEST",
    19 to "BLUETOOTH_SERVICE_PENDING",
    20 to "SOFT_AP_CALLBACK_PENDING",
)

private fun describeTetherError(code: Int?): String {
    val name = TETHER_ERROR_NAMES[code] ?: "UNKNOWN"
    return "error=$code TETHER_ERROR_$name"
}

class DownstreamControl(private val context: Context) {
    private val tetheringManager: Any
        get() = context.getSystemService("tethering")
            ?: error("tethering service unavailable")

    val opPackageName: String get() = context.opPackageName
    private val managerClass: Class<*> get() = Class.forName("android.net.TetheringManager")
    private val wifiTetheringType = 0

    fun stopWifiTethering(): Boolean = runCatching {
        val method = managerClass.getMethod("stopTethering", Int::class.javaPrimitiveType)
        method.invoke(tetheringManager, wifiTetheringType)
        Thread.sleep(STOP_SETTLE_MS)
    }.isSuccess

    fun startWifiTethering(): Pair<Boolean, String> {
        val viaTethering = startViaTetheringManager()
        if (viaTethering.first) return viaTethering
        val viaWifiManager = startViaWifiManager()
        return when {
            viaWifiManager.first -> viaWifiManager
            else -> false to "tethering[${viaTethering.second}]; wifiManager[${viaWifiManager.second}]"
        }
    }

    private fun startViaWifiManager(): Pair<Boolean, String> = runCatching {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE)
            ?: return false to "wifi service unavailable"
        val configClass = Class.forName("android.net.wifi.SoftApConfiguration")
        val method = wifiManager.javaClass.getMethod("startTetheredHotspot", configClass)
        val accepted = method.invoke(wifiManager, null) as? Boolean ?: false
        when {
            accepted -> true to "startTetheredHotspot accepted"
            else -> false to "startTetheredHotspot returned false"
        }
    }.getOrElse { failure ->
        false to "startTetheredHotspot: ${failure.cause?.message ?: failure.message}"
    }

    private fun startViaTetheringManager(): Pair<Boolean, String> {
        val exempt = startTetheringRequest(isExemptFromEntitlement = true)
        if (exempt.first) return exempt
        val plain = startTetheringRequest(isExemptFromEntitlement = false)
        return when {
            plain.first -> plain
            else -> false to "exempt[${exempt.second}]; plain[${plain.second}]"
        }
    }

    private fun startTetheringRequest(isExemptFromEntitlement: Boolean): Pair<Boolean, String> {
        val request = runCatching { buildTetheringRequest(isExemptFromEntitlement) }
            .getOrElse { return false to "buildTetheringRequest: ${it.message}" }
        val latch = CountDownLatch(1)
        val result = arrayOf("no callback")
        val callback = createStartCallback(latch, result)
            ?: return false to "could not build StartTetheringCallback proxy"
        return runCatching {
            invokeStartTethering(request, callback)
            val didComplete = latch.await(START_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            val didSucceed = didComplete && result[0] == SUCCESS
            didSucceed to result[0]
        }.getOrElse { false to "startTethering: ${it.message}" }
    }

    private fun buildTetheringRequest(isExemptFromEntitlement: Boolean): Any {
        val builderClass = Class.forName("android.net.TetheringManager\$TetheringRequest\$Builder")
        val builder = builderClass
            .getConstructor(Int::class.javaPrimitiveType)
            .newInstance(wifiTetheringType)
        if (isExemptFromEntitlement) {
            runCatching {
                builderClass
                    .getDeclaredMethod("setExemptFromEntitlementCheck", Boolean::class.java)
                    .invoke(builder, true)
            }
        }
        return builderClass.getMethod("build").invoke(builder)!!
    }

    private fun invokeStartTethering(request: Any, callback: Any) {
        val requestClass = Class.forName("android.net.TetheringManager\$TetheringRequest")
        val callbackClass = Class.forName("android.net.TetheringManager\$StartTetheringCallback")
        val method = managerClass.getMethod(
            "startTethering",
            requestClass,
            Executor::class.java,
            callbackClass,
        )
        method.invoke(tetheringManager, request, mainExecutor(), callback)
    }

    private fun createStartCallback(latch: CountDownLatch, result: Array<String>): Any? =
        runCatching {
            val callbackClass =
                Class.forName("android.net.TetheringManager\$StartTetheringCallback")
            java.lang.reflect.Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass),
            ) { _, method, args ->
                recordCallback(method.name, args, result, latch)
                null
            }
        }.getOrNull()

    private fun recordCallback(
        name: String,
        args: Array<Any?>?,
        result: Array<String>,
        latch: CountDownLatch,
    ) {
        when (name) {
            "onTetheringStarted" -> {
                result[0] = SUCCESS
                latch.countDown()
            }
            "onTetheringFailed" -> {
                val code = args?.firstOrNull() as? Int
                result[0] = "onTetheringFailed(${describeTetherError(code)})"
                latch.countDown()
            }
        }
    }

    private fun mainExecutor(): Executor {
        val handler = Handler(Looper.getMainLooper())
        return Executor { command -> handler.post(command) }
    }

    private companion object {
        const val SUCCESS = "onTetheringStarted"
        const val STOP_SETTLE_MS = 3_000L
        const val START_TIMEOUT_MS = 15_000L
    }
}

class DownstreamInspector(private val inspector: UpstreamInspector = UpstreamInspector()) {
    private var cachedCount = 0
    private var lastReadAt = 0L
    private var loggedCount = 0

    fun findTetheredDownstream(): String? {
        val observation = inspector.observe()
        if (observation.didTimeout) return "dumpsys tethering timed out; downstream state unknown"
        val tethered = observation.rawOutput.lineSequence()
            .mapNotNull { line -> TETHERED_STATE_PATTERN.find(line)?.groupValues?.get(1) }
            .distinct()
            .toList()
        return when {
            tethered.isEmpty() -> null
            else -> "downstream still tethered: $tethered"
        }
    }

    fun countDevices(): Int {
        val now = System.currentTimeMillis()
        if (now - lastReadAt < REFRESH_INTERVAL_MS) return cachedCount
        val observation = inspector.observeWifi()
        if (observation.didTimeout) return cachedCount
        lastReadAt = now
        cachedCount = parseDeviceCount(observation.rawOutput)
        logCountChange(cachedCount)
        return cachedCount
    }

    private fun logCountChange(count: Int) {
        if (count == loggedCount) return
        val direction = if (count > loggedCount) "connected" else "disconnected"
        loggedCount = count
        SessionLog.info("Client $direction: $count now on hotspot")
    }

    private fun parseDeviceCount(output: String): Int =
        CONNECTED_CLIENTS_PATTERN.findAll(output)
            .mapNotNull { match -> match.groupValues[1].toIntOrNull() }
            .maxOrNull()
            ?: 0

    private companion object {
        private val TETHERED_STATE_PATTERN =
            Regex("""^\s*(\w+)\s+-\s+TetheredState\s+-""")
        private val CONNECTED_CLIENTS_PATTERN =
            Regex("""getConnectedClientList\(\)\.size\(\):\s*(\d+)""")
        private const val REFRESH_INTERVAL_MS = 10_000L
    }
}

data class UpstreamObservation(
    val rawOutput: String,
    val interfaceNames: List<String>,
    val didTimeout: Boolean,
) {
    fun liveInterfaceNames(owned: String): List<String> {
        val filtered = interfaceNames.filter { candidate ->
            // Defunct testtun interfaces from previous sessions must never be treated as valid upstreams
            if (candidate.startsWith("testtun") && candidate != owned) {
                false
            } else {
                candidate == owned || interfaceExists(candidate)
            }
        }
        val (live, absent) = filtered.partition { it == owned || interfaceExists(it) }
        if (absent.isNotEmpty()) {
            SessionLog.warn("ignoring upstream that no longer exists: $absent")
        }
        return live
    }
}

internal fun interfaceExists(name: String): Boolean =
    runCatching {
        val ni = java.net.NetworkInterface.getByName(name)
        ni != null
    }.getOrDefault(false)

class UpstreamInspector(private val deadlineMs: Long = DEFAULT_DEADLINE_MS) {
    fun observe(): UpstreamObservation = run("tethering")
    fun observeWifi(): UpstreamObservation = run("wifi")

    private fun run(service: String): UpstreamObservation {
        val process = ProcessBuilder("dumpsys", service).start()
        val drainPool = Executors.newFixedThreadPool(2)
        val stdout = drainPool.submit<String> { process.inputStream.drain() }
        val stderr = drainPool.submit<String> { process.errorStream.drain() }
        val didExit = process.waitFor(deadlineMs, TimeUnit.MILLISECONDS)
        if (!didExit) process.destroyForcibly()
        val output = collectOutput(stdout, stderr)
        drainPool.shutdownNow()
        return UpstreamObservation(
            rawOutput = output,
            interfaceNames = parseUpstreamInterfaces(output),
            didTimeout = !didExit,
        )
    }

    private fun collectOutput(
        stdout: java.util.concurrent.Future<String>,
        stderr: java.util.concurrent.Future<String>,
    ): String {
        val out = runCatching { stdout.get(deadlineMs, TimeUnit.MILLISECONDS) }.getOrDefault("")
        val err = runCatching { stderr.get(deadlineMs, TimeUnit.MILLISECONDS) }.getOrDefault("")
        return if (err.isBlank()) out else "$out\n[stderr]\n$err"
    }

    private fun parseUpstreamInterfaces(output: String): List<String> {
        val candidateLines = output.lineSequence()
            .map(String::trim)
            .filter { line ->
                // Avoid historical dump log lines from prior sessions
                !line.contains("History", ignoreCase = true) &&
                !line.contains("Dump of", ignoreCase = true) &&
                UPSTREAM_HINTS.any { hint -> line.contains(hint, ignoreCase = true) }
            }
            .toList()

        val activeLines = if (candidateLines.size > 2) candidateLines.takeLast(2) else candidateLines
        return activeLines
            .flatMap { line -> INTERFACE_PATTERN.findAll(line).map { it.value } }
            .distinct()
            .toList()
    }

    private fun java.io.InputStream.drain(): String =
        bufferedReader().use(BufferedReader::readText)

    companion object {
        const val DEFAULT_DEADLINE_MS = 3_000L
        private val UPSTREAM_HINTS = listOf(
            "current upstream interface(s):",
            "current upstream:",
            "selected upstream:",
            "upstream network:",
            "mCurrentUpstream",
        )
        private val INTERFACE_PATTERN =
            Regex("""\b(testtun\d+|wlan\d+|rmnet[a-z_]*\d*|eth\d+|ap\d+|swlan\d+)\b""")
    }
}
