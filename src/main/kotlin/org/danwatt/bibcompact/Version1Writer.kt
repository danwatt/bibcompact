package org.danwatt.bibcompact

import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

class Version1Writer {

    fun write(verses: List<Verse>, out: OutputStream): Map<String, Int> {
        val tokenizer = VerseTokenizer()
        val tokenized = verses.map { tokenizer.tokenize(it) }.toList()

        out.write(1)
        val headerByteArray = writeHeader(verses)
        out.write(headerByteArray)

        val lexicon = Lexicon.build(tokenized)
        val lexBytes = write(lexicon)
        out.write(lexBytes)

        var textByteCount = 0
        tokenized.forEach {
            val verseBytes = write(it, lexicon)
            textByteCount += verseBytes.size
            out.write(verseBytes)
        }
        out.flush()

        return mapOf(
            "headerBytes" to headerByteArray.size,
            "lexiconBytes" to lexBytes.size,
            "textBytes" to textByteCount,
            "tokens" to lexicon.getTokens().size
        )
    }

    fun writeHeader(verses: List<Verse>): ByteArray {
        val headerBytes = mutableListOf<Byte>()
        val books = verses.map { it.book }.distinct()
        val numBooks = books.size
        // 1 byte (B) : the number of books
        headerBytes.add(numBooks.toByte())
        // B bytes (C) : the number of chapters in each book
        val verseCounts = mutableListOf<Byte>()
        books.forEach { bookNumber ->
            val chaps = verses.filter { it.book == bookNumber }.map { it.chapter }.distinct()
            headerBytes.add(chaps.size.toByte())
            chaps.forEach { chapterNumber ->
                val numVerses = verses.filter { it.book == bookNumber && it.chapter == chapterNumber }.size.toByte()
                verseCounts.add(numVerses)
            }
        }

        verseCounts.forEach { vc ->
            headerBytes.add(vc)
        }
        return headerBytes.toByteArray()
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
            position ?: throw IllegalArgumentException("Unknown token $token")
            val tokenBytes = position.toVarByte()
            bytes.addAll(tokenBytes)
        }
        return bytes.toByteArray()
    }
}