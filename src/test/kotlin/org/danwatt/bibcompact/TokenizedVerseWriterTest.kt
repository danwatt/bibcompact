package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.FileOutputStream
import java.io.FileWriter

class TokenizedVerseWriterTest {
    @Test
    fun test() {
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

        val writer = TokenizedVerseWriter(lexicon)
        val verse = TokenizedVerse(1, 1, 1, 1, listOf("First", "Second", "2", "3", "rare", "rare-er", "rare-est"))
        val bytes = writer.write(verse)
        assertThat(bytes).containsExactly(
            0b0000_0111.toByte(),//Header : 7 tokens
            0b0000_0000.toByte(),//"First", offset 0
            0b0000_0001.toByte(),//"Second", offset 1
            0b0000_0010.toByte(),//"2", offset 2
            0b0000_0011.toByte(),//"3", offset 3
            0b1000_0000.toByte(), 0b00000001.toByte(),//"rare", offset 128
            0b1000_0001.toByte(), 0b00000001.toByte(),//"rare-er", offset 129
            0b1000_0010.toByte(), 0b00000001.toByte()//"rare-est", offset 130
        )
        println(bytes.joinToString("") { "%02x".format(it) })
        //07 00 01 02 03 8001 8101 8201
    }
}