package org.danwatt.bibcompact


import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class Version3ReaderTest {

    @Test
    fun readHeader() {
        val header = "020201030102"
        val bytes = header.fromHexToByteArray()
        val headerInfo = Version3Reader().readHeader(ByteArrayInputStream(bytes))
        assertThat(headerInfo).hasSize(2)
        assertThat(headerInfo).containsSequence(
            listOf(3, 1),//Book 1
            listOf(2)//Book 2
        )
    }

    @Test
    fun readLexicon() {
        val bytes = "00000b00770484e051a401748421380167002cc060e2e0dea900".fromHexToByteArray()
        val lex = Version3Reader().readLexicon(ByteArrayInputStream(bytes))
        assertThat(lex.getTokens().map { it.token }).containsSequence("Test", "value")
    }

    @Test
    fun readSample() {
        val hex =
            "03" + //Version number
                    "020201030102" +//Book/Chapter/Verse header
                    "0000190075058209238059280a006cb2c01196004cb000a382096082580051cb047248b45c60652f0d779fa2c6cb9300" +//Lexicon
                    "00070494e949ce40a620d3139a627b4dd06f620dec4e40"//Text
        val bytes = hex.fromHexToByteArray()
        val verses = Version3Reader().read(ByteArrayInputStream(bytes))

        assertThat(verses).containsSequence(
            Verse(1001001, 1, 1, 1, "Book 1 Chapter 1 Verse 1"),
            Verse(1001002, 1, 1, 2, "Book 1 Chapter 1 Verse 2"),
            Verse(1001003, 1, 1, 3, "Book 1 Chapter 1 Verse 3"),
            Verse(1002001, 1, 2, 1, "Book 1 Chapter 2 Verse 1"),
            Verse(2001001, 2, 1, 1, "Book 2 Chapter 1 Verse 1"),
            Verse(2001002, 2, 1, 2, "Book 2 Chapter 1 Verse 2")
        )
    }

    @Test
    fun writeReadPartialCycle() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val vw = Version3Writer()
        val baos = ByteArrayOutputStream()
        vw.write(verses.filter { it.book == 1 && it.chapter == 1 }, baos)
        baos.close()

        val read = Version3Reader().read(ByteArrayInputStream(baos.toByteArray()))
        assertThat(read[0].text).isEqualTo("In the beginning God created the heaven and the earth.")
        assertThat(read[1].text).isEqualTo("And the earth was without form, and void; and darkness was upon the face of the deep. And the Spirit of God moved upon the face of the waters.")
    }

    @Test
    fun writeReadCycle() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val vw = Version3Writer()
        val baos = ByteArrayOutputStream()
        vw.write(verses, baos)
        baos.close()

        val read = Version3Reader().read(ByteArrayInputStream(baos.toByteArray()))
        assertThat(read[0].id).isEqualTo(1001001)
        assertThat(read[0].text).isEqualTo("In the beginning God created the heaven and the earth.")
        assertThat(read[1].text).isEqualTo("And the earth was without form, and void; and darkness was upon the face of the deep. And the Spirit of God moved upon the face of the waters.")

        val versesById = verses.associateBy { it.id }
        read.forEach { readVerse ->
            val originalVerse = versesById[readVerse.id]!!
            //The downloaded CSV has a couple typos - mostly extra spaces
            val originalTextWithMinorCleanup = originalVerse.text
                .replace(Regex(" +")," ")
                .replace(" ?","?")
            assertThat(readVerse.text).isEqualTo(originalTextWithMinorCleanup)
        }

    }
}