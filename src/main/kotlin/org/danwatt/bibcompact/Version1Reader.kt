package org.danwatt.bibcompact

import java.io.InputStream
import java.lang.IllegalArgumentException

class Version1Reader {
    fun read(input: InputStream): List<Verse> {
        val versionNumber = input.read()
        if (versionNumber != 1) {
            throw IllegalArgumentException("Bad version number encountered, expected 1 but was $versionNumber")
        }
        val counts = readHeader(input)
        val lex = readLexicon(input)
        return readVerses(input, counts, lex)
    }

    private fun readVerses(input: InputStream, counts: List<List<Int>>, lex: Lexicon<VerseStatsLexiconEntry>): List<Verse> {
        var counter = 1
        val verses = mutableListOf<Verse>()
        for (b in counts.indices) {
            for (c in counts[b].indices) {
                for (v in 0 until counts[b][c]) {
                    val numTokens = input.read()
                    val tokens = mutableListOf<String>()
                    for (t in 0 until numTokens) {
                        val tokenNumber = input.readVarByteInt()
                        tokens.add(lex.getTokens()[tokenNumber].token)
                    }
                    val joined = tokens.joinToString(" ")
                        .replace(" ' s ", "'s ")
                        .replace(" ( ", " (")
                        .replace("( ","(")
                        .replace(Regex(" ([).,;:!?'\"])"), "$1")
                    val verse = Verse(
                        id = (b + 1) * 1000000 + (c + 1) * 1000 + (v + 1),
                        book = b + 1,
                        chapter = c + 1,
                        verse = v + 1,
                        text = joined
                    )
                    verses.add(verse)
                    counter++
                }
            }
        }
        return verses
    }

    fun readHeader(inputStream: InputStream): List<List<Int>> {
        var headerBytesRead = 0
        val r = mutableListOf<List<Int>>()
        val bookCount = inputStream.read()
        println("There should be $bookCount books")
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
            r.add(verseCount)
        }
        return r
    }

    fun readLexicon(inputStream: InputStream): Lexicon<VerseStatsLexiconEntry> {
        val numTokens = inputStream.readInt()
        val tokens = mutableListOf<VerseStatsLexiconEntry>()

        for (t in 0 until numTokens) {
            var c: Int = -1
            val currentToken = StringBuffer()
            while (c != 0) {
                c = inputStream.read()
                if (c != 0) {
                    currentToken.append(c.toChar())
                } else {
                    tokens.add(VerseStatsLexiconEntry(token = currentToken.toString(), firstVerse = 0, lastVerse = 0))
                }
            }
        }

        return Lexicon(tokens)

    }
}

private fun InputStream.readInt() = this.read().shl(8) + this.read()
