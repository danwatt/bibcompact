package org.danwatt.bibcompact.huffman

import java.io.IOException
import java.lang.NullPointerException

/**
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
    fun write(symbol: Int) : Int {
        val bits = codeTree.getCode(symbol)
        for (b in bits) out.writeBit(b)
        return bits.size
    }
}