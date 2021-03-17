package org.danwatt.bibcompact

import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

class Version1Writer {

    fun write(verses: List<Verse>, out: OutputStream): Map<String, Int> {
        val tokenizer = VerseTokenizer()
        val tokenized = verses.map { tokenizer.tokenize(it) }.toList()
        val lexicon = Lexicon.build(tokenized)
        val lexBytes = write(lexicon)
        out.write(lexBytes)

        var textByteCount = 0;
        tokenized.forEach {
            val verseBytes = write(it, lexicon)
            textByteCount += verseBytes.size
            out.write(verseBytes)
        }
        out.flush()

        return mapOf(
            "lexiconBytes" to lexBytes.size,
            "textBytes" to textByteCount,
            "tokens" to lexicon.getTokens().size
        )
    }

    fun write(lexicon: Lexicon): ByteArray {
        val allTokens = lexicon.getTokens()
        val numTokens = allTokens.size
        val bytesNeeded = 2 + numTokens + allTokens.map { it.token.length }.sum()
        val bb = ByteBuffer.allocate(bytesNeeded)
        bb.putChar(0, numTokens.toChar())
        var position = 2
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

    fun write(verse: TokenizedVerse, lexicon: Lexicon): ByteArray {
        val bytes = mutableListOf<Byte>()
        bytes.add(verse.tokens.size.toByte())

        verse.tokens.forEach { token ->
            val position = lexicon.offset(token)
            position ?: throw IllegalArgumentException("Uknown token ${token}")
            val tokenBytes = position.toVarByte()
            /*if (tokenBytes.size == 1) {
                singleCounter[tokenBytes[0].toInt()]++
                singleByteCount++
            } else {
                doubleByteCount++
            }*/
            bytes.addAll(tokenBytes)
        }
        return bytes.toByteArray()
    }
}