package com.admin.vpnshare.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.admin.vpnshare.VpnShareApp
import rikka.shizuku.Shizuku

sealed interface ShizukuState {
    data object NotInstalled : ShizukuState
    data object NotRunning : ShizukuState
    data object PermissionRequired : ShizukuState
    data class Ready(val uid: Int, val isRoot: Boolean) : ShizukuState
}

object ShizukuGate {
    const val PERMISSION_REQUEST_CODE = 4001
    private const val SHELL_UID = 2000
    private const val ROOT_UID = 0

    private val SHIZUKU_PACKAGES = listOf(
        "moe.shizuku.privileged.api",
        "io.github.a13e300.ksu.su",
        "me.weishu.kernelsu",
        "com.topjohnwu.magisk"
    )
    private const val RELEASES_URL = "https://shizuku.rikka.app/download/"

    fun currentState(): ShizukuState {
        if (!isBinderLive()) return resolveAbsentBinder()
        if (Shizuku.isPreV11()) return ShizukuState.NotRunning
        if (!hasPermission()) return ShizukuState.PermissionRequired
        val uid = runCatching { Shizuku.getUid() }.getOrDefault(-1)
        return ShizukuState.Ready(uid = uid, isRoot = uid == ROOT_UID)
    }

    private fun isBinderLive(): Boolean {
        if (Shizuku.pingBinder()) return true
        return runCatching { Shizuku.getUid() }.isSuccess
    }

    private fun resolveAbsentBinder(): ShizukuState = when {
        isShizukuInstalled() -> ShizukuState.NotRunning
        else -> ShizukuState.NotInstalled
    }

    private fun isShizukuInstalled(): Boolean {
        val packageManager = VpnShareApp.instance.packageManager
        return SHIZUKU_PACKAGES.any { candidate -> isPackagePresent(packageManager, candidate) }
    }

    private fun isPackagePresent(packageManager: PackageManager, name: String): Boolean =
        runCatching { packageManager.getPackageInfo(name, 0) }.isSuccess

    fun hasPermission(): Boolean = runCatching {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun requestPermission() {
        runCatching {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
        }
    }

    fun remedy(context: Context, state: ShizukuState) {
        when (state) {
            ShizukuState.NotInstalled -> openReleasesPage(context)
            ShizukuState.NotRunning -> launchShizuku(context)
            ShizukuState.PermissionRequired -> requestPermission()
            is ShizukuState.Ready -> Unit
        }
    }

    private fun launchShizuku(context: Context) {
        val packageManager = context.packageManager
        val launch = SHIZUKU_PACKAGES.firstNotNullOfOrNull { candidate ->
            packageManager.getLaunchIntentForPackage(candidate)
        } ?: return openReleasesPage(context)
        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun openReleasesPage(context: Context) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun describeUid(uid: Int): String = when (uid) {
        SHELL_UID -> "Shell (UID 2000) - Shizuku Active"
        ROOT_UID -> "Root (UID 0) - Sui / Superuser Active"
        else -> "UID $uid"
    }

    fun shortUid(uid: Int): String = when (uid) {
        SHELL_UID -> "2000 (Shell)"
        ROOT_UID -> "0 (Root)"
        else -> "$uid"
    }
}
