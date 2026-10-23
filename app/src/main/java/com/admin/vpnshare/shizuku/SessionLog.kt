package com.admin.vpnshare.shizuku

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SessionLog {
    private const val TAG = "NetShareTether"
    private var isEnabled = true
    private var logFile: File? = null
    private val memoryLogs = mutableListOf<String>()

    fun useAppStorage(filesDir: File) {
        logFile = File(filesDir, "shizuku_tether.log")
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun info(message: String) {
        log("INFO", message)
    }

    fun warn(message: String) {
        log("WARN", message)
    }

    fun error(message: String) {
        log("ERROR", message)
    }

    private fun log(level: String, message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val formatted = "$timestamp [$level] $message"
        when (level) {
            "ERROR" -> Log.e(TAG, message)
            "WARN" -> Log.w(TAG, message)
            else -> Log.i(TAG, message)
        }
        if (!isEnabled) return
        synchronized(memoryLogs) {
            if (memoryLogs.size > 500) memoryLogs.removeAt(0)
            memoryLogs.add(formatted)
        }
        runCatching {
            logFile?.appendText("$formatted\n")
        }
    }

    fun getLogs(): List<String> = synchronized(memoryLogs) {
        memoryLogs.toList()
    }

    fun clear() {
        synchronized(memoryLogs) { memoryLogs.clear() }
        runCatching { logFile?.delete() }
    }
}
