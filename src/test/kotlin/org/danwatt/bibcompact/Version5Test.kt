package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.*
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

class Version5Test : VersionBaseTest(Version5Writer(readStopwordFile()), Version5Reader()) {

    /* Overrides */
    override fun generateSampleLexiconData(): Lexicon<VerseStatsLexiconEntry> {
        return Lexicon.build(
            listOf(
                TokenizedVerse(1, 1, 1, 1, listOf("Test")),
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
        Verse(1001001, 1, 1, 1, "Book 1 Chapter 1 Verse 1 the"),
        Verse(1001002, 1, 1, 2, "Book 1 Chapter 1 Verse 2 of"),
        Verse(1001003, 1, 1, 3, "Book 1 Chapter 1 Verse 3 for"),
        Verse(1002001, 1, 2, 1, "Book 1 Chapter 2 Verse 1 this"),
        Verse(2001001, 2, 1, 1, "Book 2 Chapter 1 Verse 1 a"),
        Verse(2001002, 2, 1, 2, "Book 2 Chapter 1 Verse 2"),
    )

    override fun readSampleLexicon(): Lexicon<TokenOnlyEntry> {
        //Version 5 has two lexicons. For test purposes, we are going to join them into one
        val stream = ByteArrayInputStream(writeSimpleLexicon())
        val stopLex = reader.readLexicon(stream)
        val searchLex = reader.readLexicon(stream)

        return Lexicon(stopLex.getTokens() + searchLex.getTokens())
    }

    /* Assertions */

    override fun assertTokenWriting(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("02000000060000000e000000cf80000302da8070008302fe167c04f817ff004e5dc0")
    }

    override fun assertLexiconWrite(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("00000014008103831ddc65e400f20079700aa0f1a37b0761610400000000000000001e008103801f413ba017b11971b7b11b1b003cbdd80450b151ae6451918ca3f573ef637d4000000002000201f0c0")
    }

    override fun assertHeader(headerBytes: ByteArray) {
        assertThat(headerBytes.toHex()).isEqualTo(
            "02" + // 2 books
                    "0201" + //book 1 has 2 chapters, book 2 has 1 chapter
                    "030102" // chapter 1 has 3 verses, chapter 2 has 1, chapter 3 has 2
        )
    }

    override fun assertFullSimpleWrite(stats: Map<String, Int>, byteOutput: ByteArray) {
        assertThat(stats)
            .containsEntry("headerBytes", 6)
            .containsEntry("lexiconBytes", 115)
            .containsEntry("textBytes", 45)
            .containsEntry("tokens", 11)

        //
        assertThat(byteOutput.toHex()).isEqualTo(
            "05" + //Version number
                    "020201030102" +//Book/Chapter/Verse header
                    "000000230081038dee82e6c4600b6f2007b911b9900aa0b1d89ef07c46210c6f515efea3ec4fa9b000000005000401afc00000002c0081038c88e88e814700a01bdd011e804f446dc75ec6c46e6c600a0156942caf9fd0a2a8a2acb86e5ba6eddf1b787ed508dfd1120000000006000401af78" +//Lexicon
                    "060000000900000012000000fcfcfcfcfcfc0006039ccbbbf73228000603abccaa48c246e48de9761e8c3d1b80"//Text
        )
    }

    override fun assertKjvWriteStats(stats: Map<String, Int>, rawByte: ByteArray) {
        assertThat(stats)
            .containsEntry("headerBytes", 1256)
            .containsEntry("lexiconBytes", 33732)//vs 33673 for v4
            .containsEntry("textBytes", 1004182)//vs 1000455 for v3
            .containsEntry("tokens", 13600)
        assertThat(rawByte).hasSize(1_039_171)
    }

    companion object {
        fun readStopwordFile(): Set<String> {
            val stream = BibleCsvParser::class.java.getResourceAsStream("/stopwords_kjv.txt")
            stream.bufferedReader(Charset.forName("UTF-8")).use {
                return it.readLines().asSequence()
                    .filter { line -> !line.startsWith("#") }.toSet()
            }
        }
    }
}