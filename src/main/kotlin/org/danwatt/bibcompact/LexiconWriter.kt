package org.danwatt.bibcompact

import java.nio.ByteBuffer

class LexiconWriter {
    fun write(lexicon: Lexicon): ByteArray {
        val allTokens = lexicon.getTokens()
        val numTokens = allTokens.size
        val bytesNeeded = 2 + numTokens + allTokens.map { it.token.length }.sum()
        val bb = ByteBuffer.allocate(bytesNeeded)
        bb.putChar(0, numTokens.toChar())
        var position = 2
        println("===")
        allTokens.forEach { token ->
            println(token.token)
            token.token.forEach {
                bb.put(position, it.toByte())
                position++
            }
            bb.put(position, 0x00)
            position++
        }
        println("===")

        return bb.array()
    }
}