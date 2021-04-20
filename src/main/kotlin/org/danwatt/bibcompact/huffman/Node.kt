package org.danwatt.bibcompact.huffman
/*
 * Reference Huffman coding
 * Copyright (c) Project Nayuki
 *
 * https://www.nayuki.io/page/reference-huffman-coding
 * https://github.com/nayuki/Reference-Huffman-coding
 */ /**
 * A node in a code tree. This class has exactly two subclasses: InternalNode, Leaf.
 * @see CodeTree
 */
abstract class Node  // This constructor is package-private to prevent accidental subclassing outside of this package.
internal constructor()