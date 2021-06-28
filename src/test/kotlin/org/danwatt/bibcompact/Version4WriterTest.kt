package org.danwatt.bibcompact

import org.assertj.core.api.Assertions
import org.junit.Ignore
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.nio.charset.Charset

@Ignore
class Version4WriterTest {
    @Test
    fun kjvTest() {
        val stopWords = readStopwordFile()
        val verses = BibleCsvParser().readTranslation("kjv")
        val vw = Version4Writer(stopWords)
        val byteOutput = ByteArrayOutputStream()
        val stats = vw.write(verses, byteOutput)
        byteOutput.close()
        Assertions.assertThat(stats)
            .containsEntry("headerBytes", 1256)
            .containsEntry("lexiconBytes", 61085)
            .containsEntry("textBytes", 999812)
            .containsEntry("tokens", 13600)
        val rawByte = byteOutput.toByteArray()
        Assertions.assertThat(rawByte).hasSize(1_062_154)//SO close to 1,048,576 (1MB) - 13,578 bytes

        val fw = FileOutputStream("/tmp/kjv-v4.out")
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
}