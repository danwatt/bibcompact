package org.danwatt.bibcompact

import kotlin.experimental.or

fun Int.to16bitString(): String =
    Integer.toBinaryString(this).padStart(16, '0')

fun Int.toVarByte() : List<Byte> {
    if (this < 128) {
        return listOf(this.toByte())
    } else {
        val oneTwentyEight = 128
        val highByte = oneTwentyEight.toByte() or ((this and 0b01111111).toByte())
        val lowByte = this.shr(7).toByte()
        return listOf(highByte,lowByte)
    }
}