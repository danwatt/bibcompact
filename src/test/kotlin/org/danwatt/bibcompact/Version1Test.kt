package org.danwatt.bibcompact

import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class Version1Test : VersionBaseTest(Version1Writer(), Version1Reader()) {

    override fun assertTokenWriting(bytes: ByteArray) {
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

    override fun assertLexiconWrite(bytes: ByteArray) {
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

    override fun assertHeader(headerBytes: ByteArray) {
        val result = headerBytes.toHex()

        assertThat(result).isEqualTo(
            "02" + // 2 books
                    "0201" + //book 1 has 2 chapters, book 2 has 1 chapter
                    "030102" // chapter 1 has 3 verses, chapter 2 has 1, chapter 3 has 2
        )
    }


    override fun assertFullSimpleWrite(
        stats: Map<String, Int>,
        byteOutput: ByteArray
    ) {
        assertThat(stats)
            .containsEntry("headerBytes", 6)
            .containsEntry("lexiconBytes", 27)
            .containsEntry("textBytes", 42)
            .containsEntry("tokens", 6)

        assertThat(byteOutput.toHex()).isEqualTo(
            "01" + //Version number
                    "020201030102" +//Header
                    "00063100426f6f6b00436861707465720056657273650032003300" +//Lexicon
                    "060100020003000601000200030406010002000305060100020403000601040200030006010402000304"//Tokens
        )
    }

    override fun assertKjvWriteStats(stats: Map<String, Int>, rawByte: ByteArray) {
        assertThat(stats)
            .containsEntry("headerBytes", 1256)
            .containsEntry("lexiconBytes", 109035)
            .containsEntry("textBytes", 1236508)
            .containsEntry("tokens", 13600)

        assertThat(rawByte).hasSize(1346800)
    }


    @Test
    fun writeReadCycle() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val vw = Version1Writer()
        val baos = ByteArrayOutputStream()
        vw.write(verses, baos)
        baos.close()

        val read = reader.read(ByteArrayInputStream(baos.toByteArray()))
        assertThat(read[0].id).isEqualTo(1001001)
        assertThat(read[0].text).isEqualTo("In the beginning God created the heaven and the earth.")
        assertThat(read[1].text).isEqualTo("And the earth was without form, and void; and darkness was upon the face of the deep. And the Spirit of God moved upon the face of the waters.")

        val versesById = verses.associateBy { it.id }
        read.forEach { readVerse ->
            val originalVerse = versesById[readVerse.id]!!
            //The downloaded CSV has a couple typos - mostly extra spaces
            val originalTextWithMinorCleanup = originalVerse.text
                .replace(Regex(" +"), " ")
                .replace(" ?", "?")
            assertThat(readVerse.text).isEqualTo(originalTextWithMinorCleanup)
        }

    }


    /* Some non-abstract methods */

    @Test
    fun compressParts() {
        val verses = BibleCsvParser().readTranslation("kjv")

        val tokenizer = VerseTokenizer()
        val tokenized = verses.map { tokenizer.tokenize(it) }.toList()
        val lexicon = Lexicon.build(tokenized)
        val headerBytes = writer.writeHeader(verses)
        val lexBytes = writer.writeLexicon(lexicon)

        val verseBytes = writer.writeVerseData(tokenized, lexicon)

        val algorithms = setOf(
            CompressorStreamFactory.GZIP,
            CompressorStreamFactory.XZ,
            CompressorStreamFactory.BZIP2,
            CompressorStreamFactory.DEFLATE,
            CompressorStreamFactory.LZMA
        )

        val compressionResults: Map<String, Pair<Int, Int>> = algorithms.map { algo ->
            val lb = compress(algo, lexBytes)
            val vb = compress(algo, verseBytes)

            println("Compressing the header with $algo: ${compress(algo, headerBytes).size}")

            Triple(algo, lb.size, vb.size)
        }.associateBy { it.first }.mapValues { it.value.second to it.value.third }

        assertThat(compressionResults).containsEntry(CompressorStreamFactory.GZIP, 49739 to 861500)
        assertThat(compressionResults).containsEntry(CompressorStreamFactory.DEFLATE, 49727 to 861488)
        assertThat(compressionResults).containsEntry(CompressorStreamFactory.BZIP2, 45065 to 806230)
        assertThat(compressionResults).containsEntry(CompressorStreamFactory.XZ, 43856 to 774900)
        assertThat(compressionResults).containsEntry(CompressorStreamFactory.LZMA, 43786 to 774723)
    }

    @Test
    fun lzmaTranslations() {
        val translations = setOf("asv", "bbe", "kjv", "web", "ylt")
        translations.forEach { trans ->
            val verses = BibleCsvParser().readTranslation(trans)

            val tokenizer = VerseTokenizer()
            val tokenized = verses.map { tokenizer.tokenize(it) }.toList()
            val lexicon = Lexicon.build(tokenized)
            val lexBytes = writer.writeLexicon(lexicon)
            val verseBytes = writer.writeVerseData(tokenized, lexicon)

            val lb = compress(CompressorStreamFactory.LZMA, lexBytes)
            val vb = compress(CompressorStreamFactory.LZMA, verseBytes)
            val totalSize = 1 + writer.writeHeader(verses).size + lb.size + vb.size

            println("Translation $trans: lex: ${lb.size} verse: ${vb.size}. Total: $totalSize")
        }

    }

    companion object {
        fun compress(algo: String, lexBytes: ByteArray): ByteArray {
            val b = ByteArrayOutputStream()
            val out = CompressorStreamFactory(true, 1000).createCompressorOutputStream(algo, b)
            out.write(lexBytes)
            out.flush()
            out.close()
            return b.toByteArray()
        }
    }
}