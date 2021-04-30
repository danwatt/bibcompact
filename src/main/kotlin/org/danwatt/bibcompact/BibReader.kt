package org.danwatt.bibcompact

import java.io.InputStream

abstract class BibReader(val version: Int) {
    fun read(input: InputStream): List<Verse> {
        val versionNumber = input.read()
        if (versionNumber != this.version) {
            throw IllegalArgumentException("Bad version number encountered, expected ${this.version} but was $versionNumber")
        }
        val counts = readHeader(input)
        val lex = readLexicon(input)
        return readVerses(input, counts, lex)
    }

    protected abstract fun readVerses(
        input: InputStream,
        counts: List<List<Int>>,
        lex: Lexicon<TokenOnlyEntry>
    ): List<Verse>

    fun readHeader(inputStream: InputStream): List<List<Int>> {
        var headerBytesRead = 0
        val verseCounts = mutableListOf<List<Int>>()
        val bookCount = inputStream.read()
        headerBytesRead++
        val bookChapterCounts = mutableListOf<Int>()
        for (i in 0 until bookCount) {
            val chapters = inputStream.readNBytes(1)[0].toPositiveInt()
            headerBytesRead++
            bookChapterCounts.add(chapters)
        }
        for (b in 0 until bookCount) {
            val verseCount = mutableListOf<Int>()
            for (c in 0 until bookChapterCounts[b]) {
                val v = inputStream.readNBytes(1)[0].toPositiveInt()
                verseCount.add(v)
                headerBytesRead++
            }
            verseCounts.add(verseCount)
        }
        return verseCounts
    }

    abstract fun readLexicon(inputStream: InputStream): Lexicon<TokenOnlyEntry>
    public fun applyEnglishLanguageFixesAndBuildVerse(
        tokens: List<String>,
        b: Int,
        c: Int,
        v: Int
    ): Verse {
        val joined = tokens.joinToString(" ")
            .replace(" ' s ", "'s ")
            .replace(" ( ", " (")
            .replace("( ", "(")
            .replace(Regex(" ([).,;:!?'\"])"), "$1")
        return Verse(
            id = (b + 1) * 1000000 + (c + 1) * 1000 + (v + 1),
            book = b + 1,
            chapter = c + 1,
            verse = v + 1,
            text = joined
        )
    }

}
