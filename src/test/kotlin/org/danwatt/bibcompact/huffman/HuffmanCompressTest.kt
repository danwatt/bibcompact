package org.danwatt.bibcompact.huffman

import kotlin.Throws
import java.io.*

/*
 * Reference Huffman coding
 * Copyright (c) Project Nayuki
 *
 * https://www.nayuki.io/page/reference-huffman-coding
 * https://github.com/nayuki/Reference-Huffman-coding
 */ /**
 * Tests [HuffmanCompress] coupled with [HuffmanDecompress].
 */
class HuffmanCompressTest : HuffmanCodingTest() {
    override fun compress(b: ByteArray): ByteArray {
        val freqs = FrequencyTable(IntArray(257))
        for (x in b) freqs.increment(x.toInt() and 0xFF)
        freqs.increment(256) // EOF symbol gets a frequency of 1
        var code = freqs.buildCodeTree()
        val canonCode = CanonicalCode(code, 257)
        code = canonCode.toCodeTree()
        val input: InputStream = ByteArrayInputStream(b)
        val out = ByteArrayOutputStream()
        val bitOut = BitOutputStream(out)
        HuffmanCompress.writeCodeLengthTable(bitOut, canonCode)
        HuffmanCompress.compress(code, input, bitOut)
        bitOut.close()
        return out.toByteArray()
    }

    @Throws(IOException::class)
    override fun decompress(b: ByteArray): ByteArray {
        val input: InputStream = ByteArrayInputStream(b)
        val out = ByteArrayOutputStream()
        val bitIn = BitInputStream(input)
        val canonCode = HuffmanDecompress.readCodeLengthTable(bitIn)
        val code = canonCode.toCodeTree()
        HuffmanDecompress.decompress(code, bitIn, out)
        return out.toByteArray()
    }
}