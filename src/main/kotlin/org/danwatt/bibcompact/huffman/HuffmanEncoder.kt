package org.danwatt.bibcompact.huffman

import org.danwatt.bibcompact.huffman.BitOutputStream
import org.danwatt.bibcompact.huffman.CodeTree
import kotlin.Throws
import java.io.IOException
import java.lang.NullPointerException
import java.util.*

/*
 * Reference Huffman coding
 * Copyright (c) Project Nayuki
 *
 * https://www.nayuki.io/page/reference-huffman-coding
 * https://github.com/nayuki/Reference-Huffman-coding
 */ /**
 * Encodes symbols and writes to a Huffman-coded bit stream. Not thread-safe.
 * @see HuffmanDecoder
 */
class HuffmanEncoder(val out: BitOutputStream, var codeTree: CodeTree) {

    /*---- Method ----*/
    /**
     * Encodes the specified symbol and writes to the Huffman-coded output stream.
     * @param symbol the symbol to encode, which is non-negative and must be in the range of the code tree
     * @throws IOException if an I/O exception occurred
     * @throws NullPointerException if the current code tree is `null`
     * @throws IllegalArgumentException if the symbol value is negative or has no binary code
     */
    fun write(symbol: Int) {
        val bits = codeTree.getCode(symbol)
        for (b in bits) out.write(b)
    }
}