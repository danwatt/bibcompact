package org.danwatt.bibcompact

import java.io.InputStream
import kotlin.experimental.or

fun Int.toHexByte(): String = this.toString(16).padStart(2, '0')
fun Int.toHexWord(): String = this.toString(16).padStart(4, '0')

fun Int.to16bitString(): String =
    Integer.toBinaryString(this).padStart(16, '0')

fun Int.toVarByte(): List<Byte> {
    return if (this < 128) {
        listOf(this.toByte())
    } else {
        val oneTwentyEight = 128
        val highByte = oneTwentyEight.toByte() or ((this and 0b01111111).toByte())
        val lowByte = this.shr(7).toByte()
        listOf(highByte, lowByte)
    }
}

fun Byte.toPositiveInt() = toInt() and 0xFF

fun Int.to4ByteArray(size: Int = 4): ByteArray =
    ByteArray(size) { i -> (this.toLong() shr (i * 8)).toByte() }

fun ByteArray.read32bitInt(offset: Int): Int = (this[offset + 3].toInt() shl 24) or
        (this[offset + 2].toInt() and 0xff shl 16) or
        (this[offset + 1].toInt() and 0xff shl 8) or
        (this[offset + 0].toInt() and 0xff)

fun InputStream.readVarByteInt(): Int {
    val v1 = this.read()
    if (v1 in 0..127) {
        return v1
    }

    val v2 = this.read()
    return (0b01111111 and v1) + v2.shl(7)
}


fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
fun IntArray.toHex(): String = joinToString("") { "%02x".format(it) }


fun String.fromHexToByteArray(): ByteArray {
    val len: Int = this.length
    val ba = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        ba[i / 2] = ((Character.digit(this[i], 16) shl 4)
                + Character.digit(this[i + 1], 16)).toByte()
        i += 2
    }
    return ba
}