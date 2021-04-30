package org.danwatt.bibcompact

import java.io.OutputStream

abstract class BibWriter(val version: Int) {
    fun write(verses: List<Verse>, out: OutputStream): Map<String, Int> {
        val headerByteArray = writeHeader(verses)
        val tokenizer = VerseTokenizer()
        val tokenized = verses.map { tokenizer.tokenize(it) }.toList()
        val lexicon = Lexicon.build(tokenized)
        val lexBytes = writeLexicon(lexicon)
        val textBytes = writeVerseData(tokenized, lexicon)

        out.write(this.version)
        out.write(headerByteArray)
        out.write(lexBytes)
        out.write(textBytes)
        out.flush()

        return mapOf(
            "headerBytes" to headerByteArray.size,
            "lexiconBytes" to lexBytes.size,
            "textBytes" to textBytes.size,
            "tokens" to lexicon.getTokens().size
        )
    }

    abstract fun writeVerseData(
        tokenized: List<TokenizedVerse>,
        lexicon: Lexicon<VerseStatsLexiconEntry>
    ): ByteArray

    open fun writeHeader(verses: List<Verse>): ByteArray {
        val (books, chapterCounts, verseCounts) = countBookChapterAndVerse(verses)
        return (listOf(books.size) + chapterCounts + verseCounts).map { it.toByte() }.toByteArray()
    }

    fun countBookChapterAndVerse(verses: List<Verse>): Triple<List<Int>, List<Int>, List<Int>> {
        val books = verses.map { it.book }.distinct()
        val chapterCounts = mutableListOf<Int>()
        val verseCounts = mutableListOf<Int>()
        books.forEach { bookNumber ->
            val chaps = verses.filter { it.book == bookNumber }.map { it.chapter }.distinct()
            chapterCounts.add(chaps.size)
            chaps.forEach { chapterNumber ->
                val numVerses = verses.filter { it.book == bookNumber && it.chapter == chapterNumber }.size
                verseCounts.add(numVerses)
            }
        }
        return Triple(books, chapterCounts, verseCounts)
    }

    abstract fun writeLexicon(lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray
}