package org.danwatt.bibcompact.huffman

import java.lang.IllegalArgumentException
import java.lang.AssertionError
import java.lang.StringBuilder
import java.util.*

/*
 * Reference Huffman coding
 * Copyright (c) Project Nayuki
 *
 * https://www.nayuki.io/page/reference-huffman-coding
 * https://github.com/nayuki/Reference-Huffman-coding
 */ /**
 * A binary tree that represents a mapping between symbols and binary strings.
 * The data structure is immutable. There are two main uses of a code tree:
 *
 *  * Read the root field and walk through the tree to extract the desired information.
 *  * Call getCode() to get the binary code for a particular encodable symbol.
 *
 *
 * The path to a leaf node determines the leaf's symbol's code. Starting from the root, going
 * to the left child represents a 0, and going to the right child represents a 1. Constraints:
 *
 *  * The root must be an internal node, and the tree is finite.
 *  * No symbol value is found in more than one leaf.
 *  * Not every possible symbol value needs to be in the tree.
 *
 *
 * Illustrated example:
 * <pre>  Huffman codes:
 * 0: Symbol A
 * 10: Symbol B
 * 110: Symbol C
 * 111: Symbol D
 *
 * Code tree:
 * .
 * / \
 * A   .
 * / \
 * B   .
 * / \
 * C   D</pre>
 * @see FrequencyTable
 *
 * @see CanonicalCode
 */
class CodeTree(val root: InternalNode, symbolLimit: Int) {

    // Stores the code for each symbol, or null if the symbol has no code.
    // For example, if symbol 5 has code 10011, then codes.get(5) is the list [1,0,0,1,1].
    private val codes: MutableList<List<Int>?>

    // Recursive helper function for the constructor
    private fun buildCodeList(node: Node, prefix: MutableList<Int>) {
        when (node) {
            is InternalNode -> {
                prefix.add(0)
                buildCodeList(node.left, prefix)
                prefix.removeAt(prefix.size - 1)
                prefix.add(1)
                buildCodeList(node.right, prefix)
                prefix.removeAt(prefix.size - 1)
            }
            is Leaf -> {
                require(node.symbol < codes.size) { "Symbol exceeds symbol limit" }
                require(codes[node.symbol] == null) { "Symbol has more than one code" }
                codes[node.symbol] = ArrayList(prefix)
            }
            else -> {
                throw AssertionError("Illegal node type")
            }
        }
    }
    /*---- Various methods ----*/
    /**
     * Returns the Huffman code for the specified symbol, which is a list of 0s and 1s.
     * @param symbol the symbol to query
     * @return a list of 0s and 1s, of length at least 1
     * @throws IllegalArgumentException if the symbol is negative, or no
     * Huffman code exists for it (e.g. because it had a zero frequency)
     */
    fun getCode(symbol: Int): List<Int> = when {
        symbol < 0 -> throw IllegalArgumentException("Illegal symbol")
        codes[symbol] == null -> throw IllegalArgumentException("No code for given symbol")
        else -> codes[symbol]!!
    }

    /**
     * Returns a string representation of this code tree,
     * useful for debugging only, and the format is subject to change.
     * @return a string representation of this code tree
     */
    override fun toString(): String {
        val sb = StringBuilder()
        toString("", root, sb)
        return sb.toString()
    }

    companion object {
        // Recursive helper function for toString()
        private fun toString(prefix: String, node: Node, sb: StringBuilder) {
            when (node) {
                is InternalNode -> {
                    toString(prefix + "0", node.left, sb)
                    toString(prefix + "1", node.right, sb)
                }
                is Leaf -> sb.append(String.format("Code %s: Symbol %d%n", prefix, node.symbol))
                else -> throw AssertionError("Illegal node type")

            }
        }
    }

    /**
     * Constructs a code tree from the specified tree of nodes and specified symbol limit.
     * Each symbol in the tree must have value strictly less than the symbol limit.
     * @param root the root of the tree
     * @param symbolLimit the symbol limit
     * @throws NullPointerException if tree root is `null`
     * @throws IllegalArgumentException if the symbol limit is less than 2, any symbol in the tree has
     * a value greater or equal to the symbol limit, or a symbol value appears more than once in the tree
     */
    init {
        require(symbolLimit >= 2) { "At least 2 symbols needed" }
        codes = ArrayList() // Initially all null
        for (i in 0 until symbolLimit) codes.add(null)
        buildCodeList(root, ArrayList()) // Fill 'codes' with appropriate data
    }
}