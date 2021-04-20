package org.danwatt.bibcompact.huffman

import java.util.*

/*
 * Reference Huffman coding
 * Copyright (c) Project Nayuki
 *
 * https://www.nayuki.io/page/reference-huffman-coding
 * https://github.com/nayuki/Reference-Huffman-coding
 */ /**
 * An internal node in a code tree. It has two nodes as children. Immutable.
 * @see CodeTree
 */
class InternalNode(val left: Node, val right: Node) : Node()