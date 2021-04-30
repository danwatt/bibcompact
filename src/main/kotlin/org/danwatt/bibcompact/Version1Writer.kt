package org.danwatt.bibcompact

import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

class Version1Writer : BibWriter(1) {

    override fun writeVerseData(
        tokenized: List<TokenizedVerse>,
        lexicon: Lexicon<VerseStatsLexiconEntry>
    ): ByteArray {
        var textByteCount = 0
        val byteOutput = ByteArrayOutputStream()

        tokenized.forEach {
            val verseBytes = writeVerse(it, lexicon)
            textByteCount += verseBytes.size
            byteOutput.write(verseBytes)
        }
        return byteOutput.toByteArray()
    }

    override fun writeLexicon(lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {
        val allTokens = lexicon.getTokens()
        val numTokens = allTokens.size
        val bytesNeeded = 2 + numTokens + allTokens.map { it.token.length }.sum()
        val byteBuffer = ByteBuffer.allocate(bytesNeeded)
        var position = 0
        byteBuffer.putChar(position, numTokens.toChar())
        position += 2
        allTokens.forEach { token ->
            token.token.forEach {
                byteBuffer.put(position, it.toByte())
                position++
            }
            byteBuffer.put(position, 0x00)
            position++
        }
        return byteBuffer.array()
    }


    fun writeVerse(verse: TokenizedVerse, lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {
        val bytes = mutableListOf<Byte>()
        bytes.add(verse.tokens.size.toByte())
        verse.tokens.forEach { token ->
            val position = lexicon.getLookupValue(token)
            position ?: throw IllegalArgumentException("Unknown token $token")
            val tokenBytes = position.toVarByte()
            bytes.addAll(tokenBytes)
        }
        return bytes.toByteArray()
    }
}