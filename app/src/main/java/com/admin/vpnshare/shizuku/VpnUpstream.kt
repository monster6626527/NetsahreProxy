package com.admin.vpnshare.shizuku

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.content.Context

enum class VpnMode { AUTO, ALWAYS, NEVER }

fun parseVpnMode(raw: String?): VpnMode =
    VpnMode.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: VpnMode.AUTO

class ConnectivityVpnLocator(private val connectivityManager: ConnectivityManager) {
    fun currentVpnHandle(): Long =
        connectivityManager.allNetworks
            .firstOrNull { network -> isVpn(network) }
            ?.networkHandle
            ?: UNBOUND

    private fun isVpn(network: Network): Boolean =
        connectivityManager.getNetworkCapabilities(network)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

    private companion object {
        const val UNBOUND = 0L
    }
}

sealed interface VpnBinding {
    data class Adopted(val handle: Long) : VpnBinding
    data class Lost(val problem: String) : VpnBinding
}

class VpnWatchdog(
    private val locator: ConnectivityVpnLocator,
    private val onBindingChanged: (VpnBinding) -> Unit,
) {
    fun adoptCurrentVpn(): Long = locator.currentVpnHandle()

    fun start() {
        // Active polling / observer
    }

    fun stop() {
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

    private companion object {
        const val UNBOUND = 0L
    }
}
