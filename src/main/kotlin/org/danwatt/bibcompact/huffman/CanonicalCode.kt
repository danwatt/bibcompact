package org.danwatt.bibcompact.huffman

import java.util.*

/**
 * A canonical Huffman code, which only describes the code length of
 * each symbol. Immutable. Code length 0 means no code for the symbol.
 *
 * The binary codes for each symbol can be reconstructed from the length information.
 * In this implementation, lexicographically lower binary codes are assigned to symbols
 * with lower code lengths, breaking ties by lower symbol values. For example:
 * <pre>  Code lengths (canonical code):
 * Symbol A: 1
 * Symbol B: 3
 * Symbol C: 0 (no code)
 * Symbol D: 2
 * Symbol E: 3
 *
 * Sorted lengths and symbols:
 * Symbol A: 1
 * Symbol D: 2
 * Symbol B: 3
 * Symbol E: 3
 * Symbol C: 0 (no code)
 *
 * Generated Huffman codes:
 * Symbol A: 0
 * Symbol D: 10
 * Symbol B: 110
 * Symbol E: 111
 * Symbol C: None
 *
 * Huffman codes sorted by symbol:
 * Symbol A: 0
 * Symbol B: 110
 * Symbol C: None
 * Symbol D: 10
 * Symbol E: 111</pre>
 * @see CodeTree
 */
class CanonicalCode {
    /*---- Fields and constructors ----*/
    private var codeLengths: IntArray

    /**
     * Constructs a canonical Huffman code from the specified array of symbol code lengths.
     * Each code length must be non-negative. Code length 0 means no code for the symbol.
     * The collection of code lengths must represent a proper full Huffman code tree.
     *
     * Examples of code lengths that result in under-full Huffman code trees:
     *
     *  * [1]
     *  * [3, 0, 3]
     *  * [1, 2, 3]
     *
     *
     * Examples of code lengths that result in correct full Huffman code trees:
     *
     *  * [1, 1]
     *  * [2, 2, 1, 0, 0, 0]
     *  * [3, 3, 3, 3, 3, 3, 3, 3]
     *
     *
     * Examples of code lengths that result in over-full Huffman code trees:
     *
     *  * [1, 1, 1]
     *  * [1, 1, 2, 2, 3, 3, 3, 3]
     *
     * @param codeLengths array of symbol code lengths
     * @throws NullPointerException if the array is `null`
     * @throws IllegalArgumentException if the array length is less than 2, any element is negative,
     * or the collection of code lengths would yield an under-full or over-full Huffman code tree
     */
    constructor(codeLengths: IntArray) {
        require(codeLengths.size >= 2) { "At least 2 symbols needed" }
        for (cl in codeLengths) {
            require(cl >= 0) { "Illegal code length" }
        }

        // Copy once and check for tree validity
        this.codeLengths = codeLengths.clone()
        Arrays.sort(this.codeLengths)
        var currentLevel = this.codeLengths[this.codeLengths.size - 1]
        var numNodesAtLevel = 0
        var i = this.codeLengths.size - 1
        while (i >= 0 && this.codeLengths[i] > 0) {
            val cl = this.codeLengths[i]
            while (cl < currentLevel) {
                require(numNodesAtLevel % 2 == 0) { "Under-full Huffman code tree" }
                numNodesAtLevel /= 2
                currentLevel--
            }
            numNodesAtLevel++
            i--
        }
        while (currentLevel > 0) {
            require(numNodesAtLevel % 2 == 0) { "Under-full Huffman code tree" }
            numNodesAtLevel /= 2
            currentLevel--
        }
        require(numNodesAtLevel >= 1) { "Under-full Huffman code tree" }
        require(numNodesAtLevel <= 1) { "Over-full Huffman code tree" }

        // Copy again
        System.arraycopy(codeLengths, 0, this.codeLengths, 0, codeLengths.size)
    }

    /**
     * Builds a canonical Huffman code from the specified code tree.
     * @param tree the code tree to analyze
     * @param symbolLimit a number greater than the maximum symbol value in the tree
     * @throws NullPointerException if the tree is `null`
     * @throws IllegalArgumentException if the symbol limit is less than 2, or a
     * leaf node in the tree has symbol value greater or equal to the symbol limit
     */
    constructor(tree: CodeTree, symbolLimit: Int) {
        require(symbolLimit >= 2) { "At least 2 symbols needed" }
        codeLengths = IntArray(symbolLimit)
        buildCodeLengths(tree.root, 0)
    }

    // Recursive helper method for the above constructor.
    private fun buildCodeLengths(node: Node, depth: Int) {
        when (node) {
            is InternalNode -> {
                buildCodeLengths(node.left, depth + 1)
                buildCodeLengths(node.right, depth + 1)
            }
            is Leaf -> {
                val symbol = node.symbol
                require(symbol < codeLengths.size) { "Symbol exceeds symbol limit" }
                // Note: CodeTree already has a checked constraint that disallows a symbol in multiple leaves
                if (codeLengths[symbol] != 0) throw AssertionError("Symbol has more than one code")
                codeLengths[symbol] = depth
            }
        }
    }
    /*---- Various methods ----*/
    /**
     * Returns the symbol limit for this canonical Huffman code.
     * Thus this code covers symbol values from 0 to symbolLimit1.
     * @return the symbol limit, which is at least 2
     */
    fun getSymbolLimit(): Int = codeLengths.size

    /**
     * Returns the code length of the specified symbol value. The result is 0
     * if the symbol has node code; otherwise the result is a positive number.
     * @param symbol the symbol value to query
     * @return the code length of the symbol, which is non-negative
     * @throws IllegalArgumentException if the symbol is out of range
     */
    fun getCodeLength(symbol: Int): Int {
        require(!(symbol < 0 || symbol >= codeLengths.size)) { "Symbol out of range" }
        return codeLengths[symbol]
    }

    /**
     * Returns the canonical code tree for this canonical Huffman code.
     * @return the canonical code tree
     */
    fun toCodeTree(): CodeTree {
        var nodes: List<Node> = ArrayList()
        for (i in max(codeLengths) downTo 0) {  // Descend through code lengths
            if (nodes.size % 2 != 0) throw AssertionError("Violation of canonical code invariants")
            val newNodes: MutableList<Node> = ArrayList()

            // Add leaves for symbols with positive code length i
            if (i > 0) {
                for (j in codeLengths.indices) {
                    if (codeLengths[j] == i) newNodes.add(Leaf(j))
                }
            }

            // Merge pairs of nodes from the previous deeper layer
            var j = 0
            while (j < nodes.size) {
                newNodes.add(InternalNode(nodes[j], nodes[j + 1]))
                j += 2
            }
            nodes = newNodes
        }
        if (nodes.size != 1) throw AssertionError("Violation of canonical code invariants")
        return CodeTree((nodes[0] as InternalNode), codeLengths.size)
    }

    companion object {
        // Returns the maximum value in the given array, which must have at least 1 element.
        private fun max(array: IntArray): Int = array.maxOrNull()!!
    }
}