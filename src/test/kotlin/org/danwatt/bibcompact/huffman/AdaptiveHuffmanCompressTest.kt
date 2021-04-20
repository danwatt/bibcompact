package org.danwatt.bibcompact.huffman

import java.io.*

/*
 * Reference Huffman coding
 * Copyright (c) Project Nayuki
 *
 * https://www.nayuki.io/page/reference-huffman-coding
 * https://github.com/nayuki/Reference-Huffman-coding
 */ /**
 * Tests [AdaptiveHuffmanCompress] coupled with [AdaptiveHuffmanDecompress].
 */
class AdaptiveHuffmanCompressTest : HuffmanCodingTest() {
    override fun compress(b: ByteArray): ByteArray {
        val input: InputStream = ByteArrayInputStream(b)
        val out = ByteArrayOutputStream()
        val bitOut = BitOutputStream(out)
        AdaptiveHuffmanCompress.compress(input, bitOut)
        bitOut.close()
        return out.toByteArray()
    }

    override fun decompress(b: ByteArray): ByteArray {
        val input: InputStream = ByteArrayInputStream(b)
        val out = ByteArrayOutputStream()
        AdaptiveHuffmanDecompress.decompress(BitInputStream(input), out)
        return out.toByteArray()
    }
}