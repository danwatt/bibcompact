package org.danwatt.bibcompact


import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class Version1ReaderTest {

    @Test
    fun readHeader() {
        val header = "020201030102"
        val bytes = header.fromHexToByteArray()
        val headerInfo = Version1Reader().readHeader(ByteArrayInputStream(bytes))
        assertThat(headerInfo).hasSize(2)
        assertThat(headerInfo).containsSequence(
            listOf(3, 1),//Book 1
            listOf(2)//Book 2
        )
    }

    @Test
    fun readLexicon() {
        val bytes = "0002546573740076616c756500".fromHexToByteArray()
        val lex = Version1Reader().readLexicon(ByteArrayInputStream(bytes))
        assertThat(lex.getTokens().map { it.token }).containsSequence("Test", "value")
    }

    @Test
    fun readSample() {
        val hex =
            "01" + //Version number
                    "020201030102" +//Header
                    "00063100426f6f6b00436861707465720056657273650032003300" +//Lexicon
                    "060100020003000601000200030406010002000305060100020403000601040200030006010402000304"//Tokens
        val bytes = hex.fromHexToByteArray()
        val verses = Version1Reader().read(ByteArrayInputStream(bytes))

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
        val vw = Version1Writer()
        val baos = ByteArrayOutputStream()
        vw.write(verses.filter { it.book == 1 && it.chapter == 1 }, baos)
        baos.close()

        val read = Version1Reader().read(ByteArrayInputStream(baos.toByteArray()))
        assertThat(read[0].text).isEqualTo("In the beginning God created the heaven and the earth.")
        assertThat(read[1].text).isEqualTo("And the earth was without form, and void; and darkness was upon the face of the deep. And the Spirit of God moved upon the face of the waters.")
    }

    @Test
    fun writeReadCycle() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val vw = Version1Writer()
        val baos = ByteArrayOutputStream()
        vw.write(verses, baos)
        baos.close()

        val read = Version1Reader().read(ByteArrayInputStream(baos.toByteArray()))
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