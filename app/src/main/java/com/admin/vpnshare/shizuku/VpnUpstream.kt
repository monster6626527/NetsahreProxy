package com.admin.vpnshare.shizuku

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.util.concurrent.atomic.AtomicBoolean

enum class VpnMode { AUTO, ALWAYS, NEVER }

fun parseVpnMode(raw: String?): VpnMode =
    VpnMode.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: VpnMode.AUTO

internal const val UNBOUND = 0L

internal data class VpnCandidate(val handle: Long, val isVpn: Boolean, val isActive: Boolean)

internal fun selectVpnHandle(candidates: List<VpnCandidate>): Long {
    val active = candidates.firstOrNull { it.isActive } ?: return UNBOUND
    if (!active.isVpn) return UNBOUND
    return active.handle
}

class ConnectivityVpnLocator(
    private val connectivityManager: ConnectivityManager,
) {
    fun currentVpnHandle(): Long = runCatching {
        selectVpnHandle(candidates())
    }.getOrElse { failure ->
        SessionLog.warn("could not read the active network: ${failure.message}")
        UNBOUND
    }

    private fun candidates(): List<VpnCandidate> {
        val active = connectivityManager.activeNetwork
        return connectivityManager.allNetworks.map { network ->
            VpnCandidate(
                handle = handleOf(network),
                isVpn = isVpn(network),
                isActive = network == active,
            )
        }
    }

    private fun isVpn(network: Network): Boolean =
        connectivityManager.getNetworkCapabilities(network)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

    private fun handleOf(network: Network): Long =
        runCatching { network.networkHandle }.getOrDefault(UNBOUND)
}

sealed interface VpnBinding {
    data class Adopted(val handle: Long) : VpnBinding
    data class Lost(val problem: String) : VpnBinding
}

class VpnWatchdog(
    private val locator: ConnectivityVpnLocator,
    private val onChange: (VpnBinding) -> Unit,
) {
    private val isRunning = AtomicBoolean(false)
    private var thread: Thread? = null
    private var boundHandle = UNBOUND
    private var consecutiveMisses = 0

    fun adoptCurrentVpn(): Long {
        boundHandle = locator.currentVpnHandle()
        return boundHandle
    }

    fun start() {
        if (!isRunning.compareAndSet(false, true)) return
        thread = Thread({ monitor() }, "vpn-watchdog").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        isRunning.set(false)
        val watcher = thread
        thread = null
        if (watcher != null && watcher != Thread.currentThread()) watcher.interrupt()
    }

    private fun monitor() {
        while (isRunning.get()) {
            val didSleep = runCatching { Thread.sleep(POLL_INTERVAL_MS) }.isSuccess
            if (!didSleep || !isRunning.get()) return
            val binding = evaluate() ?: continue
            if (binding is VpnBinding.Lost) isRunning.set(false)
            onChange(binding)
            if (binding is VpnBinding.Lost) return
        }
    }

    internal fun evaluate(): VpnBinding? {
        val observed = locator.currentVpnHandle()
        if (observed != UNBOUND) return adopt(observed)
        if (boundHandle == UNBOUND) return null
        consecutiveMisses++
        if (consecutiveMisses < MISSES_BEFORE_TEARDOWN) {
            SessionLog.warn(
                "vpn strike $consecutiveMisses/$MISSES_BEFORE_TEARDOWN: " +
                    "no VPN present, session is bound to $boundHandle",
            )
            return null
        }
        return VpnBinding.Lost(VPN_LOST_DETAIL)
    }

    private fun adopt(observed: Long): VpnBinding? {
        consecutiveMisses = 0
        if (observed == boundHandle) return null
        val previous = boundHandle
        boundHandle = observed
        SessionLog.info(adoptionMessage(previous, observed))
        return VpnBinding.Adopted(observed)
    }

    private fun adoptionMessage(previous: Long, adopted: Long): String = when (previous) {
        UNBOUND -> "vpn adopted: pinning the datapath to handle $adopted"
        else -> "vpn replaced: handle $previous is gone, re-pinning to $adopted"
    }

    private companion object {
        const val POLL_INTERVAL_MS = 5_000L
        const val MISSES_BEFORE_TEARDOWN = 2
        const val VPN_LOST_DETAIL =
            "VPN disconnected. Session stopped to keep devices protected."
    }
}

class VpnUpstream(
    private val context: Context,
    private val onLost: (String) -> Unit,
) {
    private var watchdog: VpnWatchdog? = null
    var handle = UNBOUND
        private set

    val isBound: Boolean get() = handle != UNBOUND

    private fun locator() = ConnectivityVpnLocator(context.connectivityManager())

    fun isVpnPresent(): Boolean = locator().currentVpnHandle() != UNBOUND

    fun follow(group: SessionResources) {
        val watcher = VpnWatchdog(locator()) { binding ->
            apply(group, binding)
        }
        watchdog = watcher
        val adopted = watcher.adoptCurrentVpn()
        if (adopted != UNBOUND) {
            group.bindDatapathTo(adopted)
            handle = adopted
            SessionLog.info("VPN present at start: datapath pinned to handle $adopted")
        } else {
            SessionLog.info("No active VPN active on default route: datapath unbound (direct traffic)")
        }
        watcher.start()
    }

    fun stop() {
        watchdog?.stop()
        watchdog = null
        handle = UNBOUND
    }

    private fun apply(group: SessionResources, binding: VpnBinding) {
        when (binding) {
            is VpnBinding.Adopted -> {
                group.bindDatapathTo(binding.handle)
                handle = binding.handle
            }
            is VpnBinding.Lost -> {
                SessionLog.warn("VPN lost: ${binding.problem}")
                handle = UNBOUND
                onLost(binding.problem)
            }
        }
    }
}
