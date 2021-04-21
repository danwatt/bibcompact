package org.danwatt.bibcompact.huffman

import java.io.IOException
import java.util.*

/*
 * Reference Huffman coding
 * Copyright (c) Project Nayuki
 *
 * https://www.nayuki.io/page/reference-huffman-coding
 * https://github.com/nayuki/Reference-Huffman-coding
 */ /**
 * Reads from a Huffman-coded bit stream and decodes symbols. Not thread-safe.
 * @see HuffmanEncoder
 */
class HuffmanDecoder(val input: BitInputStream, var codeTree: CodeTree) {


    /*---- Method ----*/
    /**
     * Reads from the input stream to decode the next Huffman-coded symbol.
     * @return the next symbol in the stream, which is non-negative
     * @throws IOException if an I/O exception occurred
     * @throws EOFException if the end of stream was reached before a symbol was decoded
     * @throws NullPointerException if the current code tree is `null`
     */
    @Throws(IOException::class)
    fun read(): Int {
        var currentNode = codeTree.root
        while (true) {
            val nextNode: Node = when (input.readNoEof()) {
                0 -> currentNode.left
                1 -> currentNode.right
                else -> throw AssertionError("Invalid value from readNoEof()")
            }
            currentNode =
                when (nextNode) {
                    is Leaf -> return nextNode.symbol
                    is InternalNode -> nextNode
                    else -> throw AssertionError("Illegal node type")
                }
        }
    }
}