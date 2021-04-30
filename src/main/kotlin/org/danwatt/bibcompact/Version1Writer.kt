package org.danwatt.bibcompact

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

class Version1Writer : BibWriter(1){

    override fun writeVerseData(
        tokenized: List<TokenizedVerse>,
        lexicon: Lexicon<VerseStatsLexiconEntry>
    ): ByteArray {
        var textByteCount = 0
        val out = ByteArrayOutputStream()

        tokenized.forEach {
            val verseBytes = writeVerse(it, lexicon)
            textByteCount += verseBytes.size
            out.write(verseBytes)
        }
        return out.toByteArray()
    }

    override fun writeLexicon(lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {
        val allTokens = lexicon.getTokens()
        val numTokens = allTokens.size
        val bytesNeeded = 2 + numTokens + allTokens.map { it.token.length }.sum()
        val bb = ByteBuffer.allocate(bytesNeeded)
        var position = 0
        bb.putChar(position, numTokens.toChar())
        position += 2
        allTokens.forEach { token ->
            token.token.forEach {
                bb.put(position, it.toByte())
                position++
            }
            bb.put(position, 0x00)
            position++
        }
        return bb.array()
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