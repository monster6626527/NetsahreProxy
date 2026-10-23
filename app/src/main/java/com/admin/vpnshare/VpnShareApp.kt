package com.admin.vpnshare

import android.app.Application
import android.os.Build
import com.admin.vpnshare.shizuku.SessionLog
import org.lsposed.hiddenapibypass.HiddenApiBypass

class VpnShareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        SessionLog.useAppStorage(filesDir)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                HiddenApiBypass.addHiddenApiExemptions("Landroid/net/", "L")
            }
        }
    }

    companion object {
        lateinit var instance: VpnShareApp
            private set
    }
}
