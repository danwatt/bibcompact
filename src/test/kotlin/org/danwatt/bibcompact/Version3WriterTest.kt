package org.danwatt.bibcompact

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

class Version3WriterTest {

    @Test
    fun testTokenWriting() {
        val filler = (2..127).map { VerseStatsLexiconEntry(it.toString(), it, it, 5) }
        val mostFrequent = listOf(
            VerseStatsLexiconEntry("First", 1, 1, 100),
            VerseStatsLexiconEntry("Second", 2, 2, 50)
        )
        val rare = listOf(
            VerseStatsLexiconEntry("rare", 1, 1, 3),
            VerseStatsLexiconEntry("rare-er", 2, 2, 2),
            VerseStatsLexiconEntry("rare-est", 3, 3, 1)
        )
        val tokens = mostFrequent + filler + rare
        val lexicon = Lexicon(tokens)

        val writer = Version3Writer()
        val verse = TokenizedVerse(1, 1, 1, 1, listOf("First", "Second", "2", "3", "rare", "rare-er", "rare-est"))
        val bytes = writer.writeVerseData(listOf(verse), lexicon)
        assertThat(bytes.toHex()).isEqualTo("008403bb82cdc026e02f7776053977")
    }

    @Test
    fun lexicon() {
        val lex = Lexicon.build(
            listOf(
                TokenizedVerse(1, 1, 1, 1, listOf("Test")),
                TokenizedVerse(2, 2, 2, 2, listOf("value"))
            )
        )

        val dw = Version3Writer()
        val bytes = dw.writeLexicon(lex)

        assertThat(bytes.toHex()).isEqualTo("00000b00770484e051a401748421380167002cc060e2e0dea900")
    }

    @Test
    fun header() {
        val verses = listOf(
            Verse(1, 1, 1, 1, "The first"),
            Verse(2, 1, 1, 2, "chapter"),
            Verse(3, 1, 1, 3, "of the first book"),
            Verse(4, 1, 2, 1, "Second chapter first book"),
            Verse(5, 2, 1, 1, "Second book"),
            Verse(6, 2, 1, 2, "Book 2, Chapter 1, Verse 2"),
        )

        val headerBytes = Version3Writer().writeHeader(verses)

        val result = headerBytes.toHex()

        assertThat(result).isEqualTo(
            "02" + // 2 books
                    "0201" + //book 1 has 2 chapters, book 2 has 1 chapter
                    "030102" // chapter 1 has 3 verses, chapter 2 has 1, chapter 3 has 2
        )
    }

    @Test
    fun fullSample() {
        val verses = listOf(
            Verse(1, 1, 1, 1, "Book 1 Chapter 1 Verse 1"),
            Verse(2, 1, 1, 2, "Book 1 Chapter 1 Verse 2"),
            Verse(3, 1, 1, 3, "Book 1 Chapter 1 Verse 3"),
            Verse(4, 1, 2, 1, "Book 1 Chapter 2 Verse 1"),
            Verse(5, 2, 1, 1, "Book 2 Chapter 1 Verse 1"),
            Verse(6, 2, 1, 2, "Book 2 Chapter 1 Verse 2"),
        )
        val vw = Version3Writer()
        val byteOutput = ByteArrayOutputStream()
        val stats = vw.write(verses, byteOutput)
        byteOutput.close()

        assertThat(stats)
            .containsEntry("headerBytes", 6)
            .containsEntry("lexiconBytes", 48)
            .containsEntry("textBytes", 23)
            .containsEntry("tokens", 6)

        //
        assertThat(byteOutput.toByteArray().toHex()).isEqualTo(
            "03" + //Version number
                    "020201030102" +//Book/Chapter/Verse header
                    "0000190075058209238059280a006cb2c01196004cb000a382096082580051cb047248b45c60652f0d779fa2c6cb9300" +//Lexicon
                    "00070494e949ce40a620d3139a627b4dd06f620dec4e40"//Text
        )
    }

    @Test
    fun kjvTest() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val vw = Version3Writer()
        val byteOutput = ByteArrayOutputStream()
        val stats = vw.write(verses, byteOutput)
        byteOutput.close()
        //155 bytes for the Lexicon, vs 159 for version 2
        //30663 bytes for the huffman header for the text, compared to 269 for version 2
        assertThat(stats)
            .containsEntry("headerBytes", 1256)
            .containsEntry("lexiconBytes", 38495)//v2: 61085
            .containsEntry("textBytes", 1000455)//v2: 999812
            .containsEntry("tokens", 13600)
        val rawByte = byteOutput.toByteArray()
        assertThat(rawByte).hasSize(1_040_207)//8,369 bytes under 1MB!!!!!!

        val fw = FileOutputStream("/tmp/kjv-v3.out")
        fw.write(rawByte)
        fw.close()
    }
}