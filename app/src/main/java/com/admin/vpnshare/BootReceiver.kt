package com.admin.vpnshare
 
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.admin.vpnshare.proxy.ProxyManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = context.getSharedPreferences("vpn_share_prefs", Context.MODE_PRIVATE)
            val startOnBoot = prefs.getBoolean("start_on_boot", false)
            if (startOnBoot) {
                val httpPort = prefs.getInt("http_port", 8080)
                val socksPort = prefs.getInt("socks_port", 1080)
                val protocolMode = prefs.getString("protocol_mode", "BOTH") ?: "BOTH"

                when (protocolMode) {
                    "HTTP" -> ProxyManager.startHttpProxy(context, httpPort)
                    "SOCKS5" -> ProxyManager.startSocksProxy(context, socksPort)
                    else -> {
                        ProxyManager.startHttpProxy(context, httpPort)
                        ProxyManager.startSocksProxy(context, socksPort)
                    }
                }
            }
        }
    }
}
