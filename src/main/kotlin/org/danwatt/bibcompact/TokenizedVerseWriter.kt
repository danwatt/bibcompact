package org.danwatt.bibcompact

import java.lang.IllegalArgumentException

class TokenizedVerseWriter(val lexicon: Lexicon) {
    var singleCounter = IntArray(128)
    var singleByteCount = 0
    var doubleByteCount = 0
    fun write(verse: TokenizedVerse): ByteArray {
        val bytes = mutableListOf<Byte>()
        bytes.add(verse.tokens.size.toByte())

        verse.tokens.forEach { token ->
            val position = lexicon.offset(token)
            position ?: throw IllegalArgumentException("Uknown token ${token}")
            val tokenBytes = position.toVarByte()
            if (tokenBytes.size == 1) {
                singleCounter[tokenBytes[0].toInt()]++
                singleByteCount++
            } else {
                doubleByteCount++
            }
            bytes.addAll(tokenBytes)
        }
        return bytes.toByteArray()
    }

    fun outputStats() {
        println("${singleByteCount} single bytes, ${doubleByteCount} double bytes")
        var counter = 0
        singleCounter.forEachIndexed { index, i ->
            counter+=i
            println("Position $index was used $i times, accumulated total of $counter")
        }
    }
}