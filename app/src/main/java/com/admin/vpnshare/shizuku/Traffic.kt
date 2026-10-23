package com.admin.vpnshare.shizuku

import java.io.File

data class Traffic(
    val up: Long = 0,
    val down: Long = 0,
) {
    companion object {
        fun format(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
            val pre = "KMGTPE"[exp - 1]
            return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
        }
    }
}

object InterfaceCounters {
    fun read(interfaceName: String): Traffic {
        val rx = readFile("/sys/class/net/$interfaceName/statistics/rx_bytes")
        val tx = readFile("/sys/class/net/$interfaceName/statistics/tx_bytes")
        return Traffic(up = tx, down = rx)
    }

    private fun readFile(path: String): Long = runCatching {
        File(path).readText().trim().toLong()
    }.getOrDefault(0L)
}
