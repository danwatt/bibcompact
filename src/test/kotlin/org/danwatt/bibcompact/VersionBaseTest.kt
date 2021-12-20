package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

open abstract class VersionBaseTest(internal val writer: BibWriter, internal val reader: BibReader) {

    @Test
    fun simpleTokens() {
        val (lexicon, verse) = buildSimpleTokenTestData()
        val bytes = writer.writeVerseData(listOf(verse), lexicon)
        assertTokenWriting(bytes)
    }

    open fun buildSimpleTokenTestData(): Pair<Lexicon<VerseStatsLexiconEntry>, TokenizedVerse> {
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
        val lexicon = Lexicon(mostFrequent + filler + rare)

        val verse =
            TokenizedVerse(1, 1, 1, 1, listOf("First", "Second", "2", "3", "rare", "rare-er", "rare-est"))
        return lexicon to verse
    }

    @Test
    fun simpleLexicon() {
        val bytes = writeSimpleLexicon()

        assertLexiconWrite(bytes)
    }

    fun writeSimpleLexicon(): ByteArray {
        val lex = generateSampleLexiconData()

        return writer.writeLexicon(lex)
    }

    open fun generateSampleLexiconData(): Lexicon<VerseStatsLexiconEntry> {
        return Lexicon.build(
            listOf(
                TokenizedVerse(1, 1, 1, 1, listOf("Test")),
                TokenizedVerse(2, 2, 2, 2, listOf("value"))
            )
        )
    }

    @Test
    fun readSimpleLexicon() {
        val lex = readSampleLexicon()
        assertThat(lex.getTokens().map { it.token }).containsSequence("Test", "value")
    }

    open fun readSampleLexicon() = reader.readLexicon(ByteArrayInputStream(writeSimpleLexicon()))


    @Test
    fun header() {
        val headerBytes = writeSampleHeader()

        assertHeader(headerBytes)
    }


    @Test
    fun readHeader() {
        val headerInfo = reader.readHeader(ByteArrayInputStream(writeSampleHeader()))
        assertThat(headerInfo).hasSize(2)
        assertThat(headerInfo).containsSequence(
            listOf(3, 1),//Book 1
            listOf(2)//Book 2
        )
    }


    private fun writeSampleHeader(): ByteArray {
        val verses = listOf(
            Verse(1, 1, 1, 1, "The first"),
            Verse(2, 1, 1, 2, "chapter"),
            Verse(3, 1, 1, 3, "of the first book"),
            Verse(4, 1, 2, 1, "Second chapter first book"),
            Verse(5, 2, 1, 1, "Second book"),
            Verse(6, 2, 1, 2, "Book 2, Chapter 1, Verse 2"),
        )

        return writer.writeHeader(verses)
    }

    @Test
    fun fullSample() {
        val (stats, bytes) = writeSampleVerses()
        assertFullSimpleWrite(stats, bytes)
    }

    private fun writeSampleVerses(): Pair<Map<String, Int>, ByteArray> {
        val verses = generateSampleVerseData()
        val byteOutput = ByteArrayOutputStream()
        val stats = writer.write(verses, byteOutput)
        byteOutput.close()
        val bytes = byteOutput.toByteArray()
        return Pair(stats, bytes)
    }

    open fun generateSampleVerseData() = listOf(
        Verse(1, 1, 1, 1, "Book 1 Chapter 1 Verse 1"),
        Verse(2, 1, 1, 2, "Book 1 Chapter 1 Verse 2"),
        Verse(3, 1, 1, 3, "Book 1 Chapter 1 Verse 3"),
        Verse(4, 1, 2, 1, "Book 1 Chapter 2 Verse 1"),
        Verse(5, 2, 1, 1, "Book 2 Chapter 1 Verse 1"),
        Verse(6, 2, 1, 2, "Book 2 Chapter 1 Verse 2"),
    )

    @Test
    fun readSample() {
        val (_, bytes) = writeSampleVerses()
        val verses = reader.read(ByteArrayInputStream(bytes))

        assertThat(verses).containsSequence(
            generateSampleVerseData()
        )
    }

    @Test
    fun writeReadPartialCycle() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val baos = ByteArrayOutputStream()
        writer.write(verses.filter { it.book == 1 && it.chapter == 1 }, baos)
        baos.close()

        val read = reader.read(ByteArrayInputStream(baos.toByteArray()))
        assertThat(read[0].text).isEqualTo("In the beginning God created the heaven and the earth.")
        assertThat(read[1].text).isEqualTo("And the earth was without form, and void; and darkness was upon the face of the deep. And the Spirit of God moved upon the face of the waters.")
    }

    @Test
    fun kjvTest() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val byteOutput = ByteArrayOutputStream()
        val stats = writer.write(verses, byteOutput)
        byteOutput.close()
        val rawByte = byteOutput.toByteArray()

        val fw = FileOutputStream("/tmp/kjv-v${writer.version}.out")
        fw.write(rawByte)
        fw.close()

        assertKjvWriteStats(stats, rawByte)
    }

    abstract fun assertTokenWriting(bytes: ByteArray)
    abstract fun assertLexiconWrite(bytes: ByteArray)
    abstract fun assertHeader(headerBytes: ByteArray)
    abstract fun assertFullSimpleWrite(stats: Map<String, Int>, byteOutput: ByteArray)
    abstract fun assertKjvWriteStats(stats: Map<String, Int>, rawByte: ByteArray)
}
