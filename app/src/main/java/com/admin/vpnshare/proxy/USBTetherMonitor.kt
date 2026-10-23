package com.admin.vpnshare.proxy

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.NetworkInterface
import java.util.Collections

sealed class UsbStatus {
    data class Connected(val ipAddress: String, val interfaceName: String) : UsbStatus()
    object Disconnected : UsbStatus()
}

object USBTetherMonitor {

    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _usbStatus = MutableStateFlow<UsbStatus>(UsbStatus.Disconnected)
    val usbStatus: StateFlow<UsbStatus> = _usbStatus.asStateFlow()

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        monitorScope.launch {
            while (isActive) {
                checkUsbTetherStatus()
                delay(2000L)
            }
        }
    }

    fun checkUsbTetherStatus() {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return
            var foundConnected: UsbStatus.Connected? = null

            for (iface in Collections.list(interfaces)) {
                if (!iface.isUp) continue
                val name = iface.name.lowercase()
                val isUsbInterfaceName = name.contains("rndis") || name.contains("usb") || name.contains("ncm")

                val addresses = iface.inetAddresses ?: continue
                for (addr in Collections.list(addresses)) {
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (isUsbInterfaceName || ip.startsWith("192.168.42.") || ip.startsWith("192.168.44.")) {
                            foundConnected = UsbStatus.Connected(ipAddress = ip, interfaceName = iface.name)
                            break
                        }
                    }
                }
                if (foundConnected != null) break
            }

            _usbStatus.value = foundConnected ?: UsbStatus.Disconnected
        } catch (e: Exception) {
            _usbStatus.value = UsbStatus.Disconnected
        }
    }
}
