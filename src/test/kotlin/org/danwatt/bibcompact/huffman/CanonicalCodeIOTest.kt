package org.danwatt.bibcompact.huffman

import org.assertj.core.api.Assertions.assertThat
import org.danwatt.bibcompact.toHex
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.*

class CanonicalCodeIOTest {
    @Test
    fun simple() {
        val ct = FrequencyTable(intArrayOf(1, 2, 3, 4)).buildCodeTree()
        val baos = ByteArrayOutputStream()
        val bitOut = BitOutputStream(baos)
        CanonicalCodeIO.write(CanonicalCode(ct, 4), bitOut)
        bitOut.close()
        assertThat(baos.toByteArray().toHex())
            .startsWith("000402")
            .endsWith("ff50")//111 111 110 101 -> 1111 1111 0101 |0000
    }

    @Test
    fun runLengthEncoding() {
        val ir = IntArray(128)
        Arrays.fill(ir, 100)
        val ct = FrequencyTable(ir).buildCodeTree()
        val baos = ByteArrayOutputStream()
        val bitOut = BitOutputStream(baos)
        CanonicalCodeIO.write(CanonicalCode(ct, ir.size), bitOut)
        bitOut.close()
        //7, 127 times
        assertThat(baos.toByteArray().toHex())
            .startsWith("008003")
            .endsWith("f3f8")//1111 0011 1111 1|000
    }

    @Test
    fun variationAndLargeRuns() {
        val ir = IntArray(2048)
        Arrays.fill(ir, 0, 128, 100)
        Arrays.fill(ir, 128, 2048, 4)
        val ct = FrequencyTable(ir).buildCodeTree()
        val baos = ByteArrayOutputStream()
        val bitOut = BitOutputStream(baos)
        CanonicalCodeIO.write(CanonicalCode(ct, ir.size), bitOut)
        bitOut.close()
        //7, 127 times
        assertThat(baos.size()).isEqualTo(21)
        assertThat(baos.toByteArray().toHex())
            .startsWith("080004")//0800 = 2048 codes, 4 bits for each code
            .endsWith("c1dee07e3ff8ffe3ff8ffe3ff8ffe3ff87f0")
        /*
        Code 8, run 119:  11000 + 001110111 -> 1100(C)|0001(1)|1101(D)|11...
        Code 7, run 7  :  10111 + 000000111 -> 11_10(E)|1110(E)|0000(0)|0111(7)|
        c1dee07e3ff8ffe3ff8ffe3ff8ffe3ff87f0
         */
    }
}