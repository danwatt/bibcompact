package org.danwatt.bibcompact.huffman

/*
 * Reference Huffman coding
 * Copyright (c) Project Nayuki
 *
 * https://www.nayuki.io/page/reference-huffman-coding
 * https://github.com/nayuki/Reference-Huffman-coding
 */ /**
 * A leaf node in a code tree. It has a symbol value. Immutable.
 * @see CodeTree
 */
class Leaf(sym: Int) : Node() {
    @JvmField
    val symbol // Always non-negative
            : Int

    init {
        require(sym >= 0) { "Symbol value must be non-negative" }
        symbol = sym
    }
}