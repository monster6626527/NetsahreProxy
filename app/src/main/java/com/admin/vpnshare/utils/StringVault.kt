package com.admin.vpnshare.utils

object StringVault {
    private fun xor(input: String, key: Int = 0x5A): String =
        input.map { (it.code xor key).toChar() }.joinToString("")

    // Store as XOR-encoded, decode only at point of use
    fun decode(encoded: String): String = xor(encoded)
}
