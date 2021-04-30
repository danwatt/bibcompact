package org.danwatt.bibcompact

import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

class Version1Writer {

    fun write(verses: List<Verse>, out: OutputStream): Map<String, Int> {
        out.write(1)
        val headerByteArray = writeHeader(verses)
        out.write(headerByteArray)

        val tokenizer = VerseTokenizer()
        val tokenized = verses.map { tokenizer.tokenize(it) }.toList()
        val lexicon = Lexicon.build(tokenized)
        val lexBytes = writeLexicon(lexicon)
        out.write(lexBytes)

        val textByteCount = writeVerseData(tokenized, lexicon, out)
        out.flush()

        return mapOf(
            "headerBytes" to headerByteArray.size,
            "lexiconBytes" to lexBytes.size,
            "textBytes" to textByteCount,
            "tokens" to lexicon.getTokens().size
        )
    }

    fun writeVerseData(
        tokenized: List<TokenizedVerse>,
        lexicon: Lexicon<VerseStatsLexiconEntry>,
        out: OutputStream
    ): Int {
        var textByteCount = 0

        tokenized.forEach {
            val verseBytes = writeVerse(it, lexicon)
            textByteCount += verseBytes.size
            out.write(verseBytes)
        }
        return textByteCount
    }

    fun writeHeader(verses: List<Verse>): ByteArray {
        val books = verses.map { it.book }.distinct()
        val chapterCounts = mutableListOf<Byte>()
        val verseCounts = mutableListOf<Byte>()
        books.forEach { bookNumber ->
            val chaps = verses.filter { it.book == bookNumber }.map { it.chapter }.distinct()
            chapterCounts.add(chaps.size.toByte())
            chaps.forEach { chapterNumber ->
                val numVerses = verses.filter { it.book == bookNumber && it.chapter == chapterNumber }.size.toByte()
                verseCounts.add(numVerses)
            }
        }

        return (listOf(books.size.toByte()) + chapterCounts + verseCounts).toByteArray()
    }

    fun writeLexicon(lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {
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