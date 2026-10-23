package com.admin.vpnshare.shizuku

import android.content.Context

class SessionTeardown(private val context: Context) {
    private var shutdownHook: Thread? = null

    fun installShutdownHook() {
        val hook = Thread {
            SessionLog.info("JVM shutdown hook fired: ensuring tethering downstream is stopped")
            releaseDownstream()
        }
        shutdownHook = hook
        Runtime.getRuntime().addShutdownHook(hook)
    }

    fun removeShutdownHook() {
        shutdownHook?.let { hook ->
            runCatching { Runtime.getRuntime().removeShutdownHook(hook) }
        }
        shutdownHook = null
    }

    fun releaseDownstream(): String? {
        val control = DownstreamControl(context)
        control.stopWifiTethering()
        return null
    }

    fun releaseUpstreamSelection(interfaceName: String?) {
        runCatching {
            TetheringPreferenceApi(context).setPreferTestNetworks(false)
        }.onFailure {
            SessionLog.warn("could not reset test network preference: ${it.message}")
        }
    }
}

class SessionWatchdog(
    private val expectedInterface: String,
    private val onDrift: (String) -> Unit,
) {
    private var running = false
    private var thread: Thread? = null

    fun start() {
        running = true
        thread = Thread {
            val inspector = UpstreamInspector()
            while (running) {
                Thread.sleep(5000)
                if (!running) break
                val observation = inspector.observe()
                val upstreams = observation.liveInterfaceNames(expectedInterface)
                if (upstreams.isNotEmpty() && !upstreams.contains(expectedInterface)) {
                    onDrift("Upstream drifted away from $expectedInterface to $upstreams")
                    break
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
    }
}
