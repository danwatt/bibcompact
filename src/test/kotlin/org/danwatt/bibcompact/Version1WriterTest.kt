package org.danwatt.bibcompact

import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

class Version1WriterTest {

    @Test
    fun testTokenWriting() {
        val filler = (2..127).map { LexiconEntry(it.toString(), it, it, 5) }
        val mostFrequent = listOf(
            LexiconEntry("First", 1, 1, 100),
            LexiconEntry("Second", 2, 2, 50)
        )
        val rare = listOf(
            LexiconEntry("rare", 1, 1, 3),
            LexiconEntry("rare-er", 2, 2, 2),
            LexiconEntry("rare-est", 3, 3, 1)
        )
        val tokens = mostFrequent + filler + rare
        val lexicon = Lexicon(tokens)

        val writer = Version1Writer()
        val verse = TokenizedVerse(1, 1, 1, 1, listOf("First", "Second", "2", "3", "rare", "rare-er", "rare-est"))
        val bytes = writer.write(verse, lexicon)
        assertThat(bytes.toHex()).isEqualTo(
            "07" +//7 tokens
                    "00" +//"First", offset 0
                    "01" +//"Second", offset 1
                    "02" +//"2", offset 2
                    "03" +//"3", offset 3
                    "8001" +//"rare", offset 128
                    "8101" +//"rare-er", offset 129
                    "8201" //"rare-est", offset 130
        )
    }

    @Test
    fun lexicon() {
        val lex = Lexicon.build(
            listOf(
                TokenizedVerse(1, 1, 1, 1, listOf("Test")),
                TokenizedVerse(2, 2, 2, 2, listOf("value"))
            )
        )

        val dw = Version1Writer()
        val bytes = dw.write(lex)

        assertThat(bytes.toHex()).isEqualTo("0002546573740076616c756500")

        assertThat(bytes).containsSequence(
            0x00,
            0x02,
            'T'.toByte(),
            'e'.toByte(),
            's'.toByte(),
            't'.toByte(),
            0x00,
            'v'.toByte(),
            'a'.toByte(),
            'l'.toByte(),
            'u'.toByte(),
            'e'.toByte(),
            0x00
        )
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

        val headerBytes = Version1Writer().writeHeader(verses)

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
        val vw = Version1Writer()
        val baos = ByteArrayOutputStream()
        val stats = vw.write(verses, baos)
        baos.close()

        assertThat(stats)
            .containsEntry("headerBytes", 6)
            .containsEntry("lexiconBytes", 27)
            .containsEntry("textBytes", 42)
            .containsEntry("tokens", 6)

        assertThat(baos.toByteArray().toHex()).isEqualTo(
            "01" + //Version number
                    "020201030102" +//Header
                    "00063100426f6f6b00436861707465720056657273650032003300" +//Lexicon
                    "060100020003000601000200030406010002000305060100020403000601040200030006010402000304"//Tokens
        )
    }

    @Test
    fun kjvTest() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val vw = Version1Writer()
        val baos = ByteArrayOutputStream()
        val stats = vw.write(verses, baos)
        baos.close()
        assertThat(stats)
            .containsEntry("headerBytes", 1256)
            .containsEntry("lexiconBytes", 109035)
            .containsEntry("textBytes", 1236508)
            .containsEntry("tokens", 13600)
        val rawByte = baos.toByteArray()
        assertThat(rawByte).hasSize(1346800)

        val fw = FileOutputStream("/tmp/kjv.out")
        fw.write(rawByte)
        fw.close()

        val xzBytes = xzCompress(rawByte)
        assertThat(xzBytes).hasSize(819652)

    }

    private fun xzCompress(rawByte: ByteArray): ByteArray {
        val xzByteOut = ByteArrayOutputStream()
        val xzOut = XZCompressorOutputStream(xzByteOut, 9)
        xzOut.write(rawByte)
        xzOut.finish()
        xzOut.close()
        return xzByteOut.toByteArray()
    }
}

