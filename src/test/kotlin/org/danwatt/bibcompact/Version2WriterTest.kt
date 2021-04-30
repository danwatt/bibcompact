package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.compressors.CompressorStreamFactory.*


class Version2WriterTest {

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

        val writer = Version2Writer()
        val verse = TokenizedVerse(1, 1, 1, 1, listOf("First", "Second", "2", "3", "rare", "rare-er", "rare-est"))
        val bytes = writer.writeVerseData(listOf(verse), lexicon)
        assertThat(bytes.toHex()).isEqualTo("008403bbbb83ddddd8053977")
    }

    @Test
    fun lexicon() {
        val lex = Lexicon.build(
            listOf(
                TokenizedVerse(1, 1, 1, 1, listOf("Test")),
                TokenizedVerse(2, 2, 2, 2, listOf("value"))
            )
        )

        val dw = Version2Writer()
        val bytes = dw.writeLexicon(lex)

        assertThat(bytes.toHex()).isEqualTo("0077049c0a54802e908427002ce005980c0002e2e0dea900")
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
        val vw = Version2Writer()
        val baos = ByteArrayOutputStream()
        val stats = vw.write(verses, baos)
        baos.close()

        assertThat(stats)
            .containsEntry("headerBytes", 6)
            .containsEntry("lexiconBytes", 44)
            .containsEntry("textBytes", 23)
            .containsEntry("tokens", 6)

        //
        assertThat(baos.toByteArray().toHex()).isEqualTo(
            "02" + //Version number
                    "020201030102" +//Book/Chapter/Verse header
                    "0075058a017ca02801b2cb0046580132c0028e082582096001492c11c9200006b1988f1aef3f499b4e517300" +//Lexicon
                    "00070494e73a52408530c29b98537b0bd867530cea6e40"//Text
        )
    }

    @Test
    fun kjvTest() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val vw = Version2Writer()
        val baos = ByteArrayOutputStream()
        val stats = vw.write(verses, baos)
        baos.close()
        assertThat(stats)
            .containsEntry("headerBytes", 1256)
            .containsEntry("lexiconBytes", 61085)
            .containsEntry("textBytes", 999812)
            .containsEntry("tokens", 13600)
        val rawByte = baos.toByteArray()
        assertThat(rawByte).hasSize(1_062_154)//SO close to 1,048,576 (1MB) - 13,578 bytes

        val fw = FileOutputStream("/tmp/kjv-v2.out")
        fw.write(rawByte)
        fw.close()
    }

    @Test
    fun lzmaTranslations() {
        val translations = setOf("asv", "bbe", "kjv", "web", "ylt")
        translations.forEach { trans ->
            val verses = BibleCsvParser().readTranslation(trans)
            val vw = Version2Writer()

            val tokenizer = VerseTokenizer()
            val tokenized = verses.map { tokenizer.tokenize(it) }.toList()
            val lexicon = Lexicon.build(tokenized)
            val lexBytes = vw.writeLexicon(lexicon)

            val verseBytes = vw.writeVerseData(tokenized, lexicon)

            val lb = compress(LZMA, lexBytes)
            val vb = compress(LZMA, verseBytes)

            val totalSize = 1 + vw.writeHeader(verses).size + lb.size + vb.size

            println("Translation $trans: lex: ${lb.size} verse: ${vb.size}. Total: ${totalSize}")

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


