package org.danwatt.bibcompact.huffman

import org.assertj.core.api.Assertions.assertThat
import org.danwatt.bibcompact.toHex
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

class CanonicalCodeIOTest {
    @Test
    fun simple() {
        val freqs = intArrayOf(1, 2, 3, 4)
        val ct = FrequencyTable(freqs).buildCodeTree()
        val baos = ByteArrayOutputStream()
        val bitOut = BitOutputStream(baos)
        val originalCode = CanonicalCode(ct, freqs.size)
        CanonicalCodeIO.write(originalCode, bitOut)
        bitOut.close()
        assertThat(baos.toByteArray().toHex())
            .startsWith("000403")
            .endsWith("bba9")

        assertDecode(baos, originalCode, freqs)
    }

    private fun assertDecode(
        baos: ByteArrayOutputStream,
        originalCode: CanonicalCode,
        freqs: IntArray
    ) {
        val decoded = CanonicalCodeIO.read(BitInputStream(ByteArrayInputStream(baos.toByteArray())))
        assertThat(decoded.getSymbolLimit()).isEqualTo(freqs.size)
        val ot = originalCode.toCodeTree()
        val dt = decoded.toCodeTree()
        for (i in freqs.indices) {
            assertThat(ot.getCode(i)).isEqualTo(dt.getCode(i)).describedAs("Code number $i")
        }
    }

    @Test
    fun runLengthEncoding() {
        val freqs = IntArray(128)
        Arrays.fill(freqs, 100)
        val ct = FrequencyTable(freqs).buildCodeTree()
        val baos = ByteArrayOutputStream()
        val bitOut = BitOutputStream(baos)
        val originalCode = CanonicalCode(ct, freqs.size)
        CanonicalCodeIO.write(originalCode, bitOut)
        bitOut.close()
        //7, 127 times
        assertThat(baos.toByteArray().toHex())
            .startsWith("008007")
            .endsWith("873f80")
        assertDecode(baos, originalCode, freqs)
    }

    @Test
    fun variationAndLargeRuns() {
        val freqs = IntArray(2048)
        Arrays.fill(freqs, 0, 128, 100)
        Arrays.fill(freqs, 128, 2048, 4)
        val ct = FrequencyTable(freqs).buildCodeTree()
        val baos = ByteArrayOutputStream()
        val bitOut = BitOutputStream(baos)
        val originalCode = CanonicalCode(ct, freqs.size)
        CanonicalCodeIO.write(originalCode, bitOut)
        bitOut.close()
        //7, 127 times
        assertThat(baos.size()).isEqualTo(31)
        assertThat(baos.toByteArray().toHex())
            .startsWith("08000c")//0800 = 2048 codes, 12 bits for each code
            .endsWith("8041de00e078063fe018ff8063fe018ff8063fe018ff8063fe0187f0")
        assertDecode(baos, originalCode, freqs)
    }
}