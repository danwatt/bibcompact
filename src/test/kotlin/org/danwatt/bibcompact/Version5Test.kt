package org.danwatt.bibcompact

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import java.nio.charset.Charset

class Version5Test : VersionBaseTest(Version5Writer(readStopwordFile()), Version5Reader()) {

    /* Overrides */
    override fun generateSampleLexiconData(): Lexicon<VerseStatsLexiconEntry> {
        return Lexicon.build(
            listOf(
                TokenizedVerse(1, 1, 1, 1, listOf("Test")),
                TokenizedVerse(1, 1, 1, 1, listOf("of")),
                TokenizedVerse(2, 2, 2, 2, listOf("the")),
                TokenizedVerse(2, 2, 2, 2, listOf("value"))
            )
        )
    }

    override fun buildSimpleTokenTestData(): Pair<Lexicon<VerseStatsLexiconEntry>, TokenizedVerse> {
        val filler = (2..127).map { VerseStatsLexiconEntry(it.toString(), it, it, 5) }
        val mostFrequent = listOf(
            VerseStatsLexiconEntry("First", 1, 1, 100),
            VerseStatsLexiconEntry("Second", 2, 2, 50),
            VerseStatsLexiconEntry("the", 2, 2, 30),
            VerseStatsLexiconEntry("of", 2, 2, 20),
        )
        val rare = listOf(
            VerseStatsLexiconEntry("rare", 1, 1, 3),
            VerseStatsLexiconEntry("rare-er", 2, 2, 2),
            VerseStatsLexiconEntry("rare-est", 3, 3, 1)
        )
        val tokens = mostFrequent + filler + rare
        val lexicon = Lexicon(tokens)

        val verse =
            TokenizedVerse(1, 1, 1, 1, listOf("First", "Second", "the", "of", "2", "3", "rare", "rare-er", "rare-est"))
        return Pair(lexicon, verse)
    }

    override fun generateSampleVerseData() = listOf(
        Verse(1, 1, 1, 1, "Book 1 Chapter 1 Verse 1 the"),
        Verse(2, 1, 1, 2, "Book 1 Chapter 1 Verse 2 of"),
        Verse(3, 1, 1, 3, "Book 1 Chapter 1 Verse 3 for"),
        Verse(4, 1, 2, 1, "Book 1 Chapter 2 Verse 1 this"),
        Verse(5, 2, 1, 1, "Book 2 Chapter 1 Verse 1 a"),
        Verse(6, 2, 1, 2, "Book 2 Chapter 1 Verse 2"),
    )

    /* Assertions */

    override fun assertTokenWriting(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("008303bb82cdc026e02f77404e5dc0000201f02000")
    }

    override fun assertLexiconWrite(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("000200810582096030491cb2c72c0039248005247002a2d151fbf0a32319467791a0000301ecc0000200810680038d009d1a002e2c081121c08b0e2c081160458003890e2c58008840b151ae6451918ca3f573ef637d40000301ecc0")
    }

    override fun assertHeader(headerBytes: ByteArray) {
        assertThat(headerBytes.toHex()).isEqualTo(
            "02" + // 2 books
                    "0201" + //book 1 has 2 chapters, book 2 has 1 chapter
                    "030102" // chapter 1 has 3 verses, chapter 2 has 1, chapter 3 has 2
        )
    }

    override fun assertFullSimpleWrite(stats: Map<String, Int>, byteOutput: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun assertKjvWriteStats(stats: Map<String, Int>, rawByte: ByteArray) {
        assertThat(stats)
            .containsEntry("headerBytes", 1256)
            .containsEntry("lexiconBytes", 33952)//vs 33673 for v4
            .containsEntry("textBytes", 947784)//vs 1000455 for v3
            .containsEntry("tokens", 13600)
        assertThat(rawByte).hasSize(982_993)
    }

    companion object {
        private fun readStopwordFile(): Set<String> {
            val stream = BibleCsvParser::class.java.getResourceAsStream("/stopwords_kjv.txt")
            stream.bufferedReader(Charset.forName("UTF-8")).use {
                return it.readLines().asSequence()
                    .filter { line -> !line.startsWith("#") }.toSet()
            }
        }
    }
}