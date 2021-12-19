package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.*
import org.junit.Ignore
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.nio.charset.Charset

@Ignore
class Version5WriterTest {
    val stopWords = readStopwordFile()
    val writer = Version5Writer(stopWords)

    @Test
    fun kjvTest() {

        val verses = BibleCsvParser().readTranslation("kjv")

        val byteOutput = ByteArrayOutputStream()
        val stats = writer.write(verses, byteOutput)
        byteOutput.close()
        assertThat(stats)
            .containsEntry("headerBytes", 1256)
            .containsEntry("lexiconBytes", 33952)//vs 33673 for v4
            .containsEntry("textBytes", 947784)//vs 1000455 for v3
            .containsEntry("tokens", 13600)
        val rawByte = byteOutput.toByteArray()
        assertThat(rawByte).hasSize(982_993)

        val fw = FileOutputStream("/tmp/kjv-v5.out")
        fw.write(rawByte)
        fw.close()
    }

    private fun readStopwordFile(): Set<String> {
        val stream = BibleCsvParser::class.java.getResourceAsStream("/stopwords_kjv.txt")
        stream.bufferedReader(Charset.forName("UTF-8")).use {
            return it.readLines().asSequence()
                .filter { line -> !line.startsWith("#") }.toSet()
        }

    }

    @Test
    fun stopWordExceptionCompression() {
        val SWM = 0
        //In the beginning God created the heaven and the earth.
        //X  X   SWM       SWM SWM     X   SWM    X   X   SWM  X
        val stopWordList = listOf(1, 2, SWM, SWM, SWM, 4, SWM, 5, 6, SWM, 7)

        val compressed = writer.compressStopWordListing(stopWordList)
    }
}