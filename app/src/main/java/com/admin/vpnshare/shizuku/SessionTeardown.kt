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
    private val context: Context,
    private val expectedInterface: String,
    private val onDrift: (String) -> Unit,
) {
    private var running = false
    private var thread: Thread? = null
    private var consecutiveDriftStrikes = 0

    fun start() {
        running = true
        thread = Thread({
            val inspector = UpstreamInspector()
            val tetherPref = TetheringPreferenceApi(context)
            while (running) {
                val didSleep = runCatching { Thread.sleep(5000) }.isSuccess
                if (!didSleep || !running) break
                val observation = inspector.observe()
                if (observation.didTimeout) continue

                val upstreams = observation.liveInterfaceNames(expectedInterface)
                val ownedStillAlive = interfaceExists(expectedInterface)
                val isOwnedInUpstream = upstreams.contains(expectedInterface)

                if (isOwnedInUpstream || upstreams.isEmpty()) {
                    consecutiveDriftStrikes = 0
                    continue
                }

                // Discard any ghost testtun interfaces (only genuine network interfaces like wlan0 or rmnet0 matter)
                val realExternalUpstreams = upstreams.filter { !it.startsWith("testtun") }
                if (realExternalUpstreams.isEmpty()) {
                    consecutiveDriftStrikes = 0
                    continue
                }

                consecutiveDriftStrikes++
                SessionLog.warn("Watchdog drift strike $consecutiveDriftStrikes/3: observed $realExternalUpstreams instead of $expectedInterface")

                // Re-assert test network preference on strikes
                if (ownedStillAlive) {
                    runCatching { tetherPref.setPreferTestNetworks(true) }
                }

                if (consecutiveDriftStrikes >= 3 && !ownedStillAlive) {
                    running = false
                    onDrift("Upstream drifted away from $expectedInterface to $realExternalUpstreams")
                    break
                }
            }
        }, "SessionWatchdog").apply {
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
