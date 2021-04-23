package org.danwatt.bibcompact.huffman

/**
 * A node in a code tree. This class has exactly two subclasses: InternalNode, Leaf.
 * @see CodeTree
 */
sealed class Node

/**
 * A leaf node in a code tree. It has a symbol value. Immutable.
 * @see CodeTree
 */
class Leaf(val symbol: Int) : Node() {
    init {
        require(symbol >= 0) { "Symbol value must be non-negative" }
    }
}

/**
 * An internal node in a code tree. It has two nodes as children. Immutable.
 * @see CodeTree
 */
class InternalNode(val left: Node, val right: Node) : Node()