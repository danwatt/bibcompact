package org.danwatt.bibcompact.huffman

import java.util.*
import kotlin.math.min

/**
 * A table of symbol frequencies. Mutable and not thread-safe. Symbols values are
 * numbered from 0 to symbolLimit1. A frequency table is mainly used like this:
 *
 *  1. Collect the frequencies of symbols in the stream that we want to compress.
 *  1. Build a code tree that is statically optimal for the current frequencies.
 *
 *
 * This implementation is designed to avoid arithmetic overflow - it correctly builds
 * an optimal code tree for any legal number of symbols (2 to `Integer.MAX_VALUE`),
 * with each symbol having a legal frequency (0 to `Integer.MAX_VALUE`).
 * @see CodeTree
 */
class FrequencyTable(freqs: IntArray) {
    /*---- Field and constructor ----*/ // Length at least 2, and every element is non-negative.
    private val frequencies: IntArray
    /*---- Basic methods ----*/
    /**
     * Returns the number of symbols in this frequency table. The result is always at least 2.
     * @return the number of symbols in this frequency table
     */
    fun getSymbolLimit(): Int {
        return frequencies.size
    }

    /**
     * Returns the frequency of the specified symbol in this frequency table. The result is always non-negative.
     * @param symbol the symbol to query
     * @return the frequency of the specified symbol
     * @throws IllegalArgumentException if the symbol is out of range
     */
    operator fun get(symbol: Int): Int {
        checkSymbol(symbol)
        return frequencies[symbol]
    }

    /**
     * Sets the frequency of the specified symbol in this frequency table to the specified value.
     * @param symbol the symbol whose frequency will be modified
     * @param freq the frequency to set it to, which must be non-negative
     * @throws IllegalArgumentException if the symbol is out of range or the frequency is negative
     */
    operator fun set(symbol: Int, freq: Int) {
        checkSymbol(symbol)
        require(freq >= 0) { "Negative frequency" }
        frequencies[symbol] = freq
    }

    /**
     * Increments the frequency of the specified symbol in this frequency table.
     * @param symbol the symbol whose frequency will be incremented
     * @throws IllegalArgumentException if the symbol is out of range
     * @throws IllegalStateException if the symbol already has
     * the maximum allowed frequency of `Integer.MAX_VALUE`
     */
    fun increment(symbol: Int) {
        checkSymbol(symbol)
        check(frequencies[symbol] != Int.MAX_VALUE) { "Maximum frequency reached" }
        frequencies[symbol]++
    }

    // Returns silently if 0 <= symbol < frequencies.length, otherwise throws an exception.
    private fun checkSymbol(symbol: Int) {
        require(!(symbol < 0 || symbol >= frequencies.size)) { "Symbol out of range" }
    }

    /**
     * Returns a string representation of this frequency table,
     * useful for debugging only, and the format is subject to change.
     * @return a string representation of this frequency table
     */
    override fun toString(): String {
        val sb = StringBuilder()
        for (i in frequencies.indices) sb.append(String.format("%d\t%d%n", i, frequencies[i]))
        return sb.toString()
    }
    /*---- Advanced methods ----*/
    /**
     * Returns a code tree that is optimal for the symbol frequencies in this table.
     * The tree always contains at least 2 leaves (even if they come from symbols with
     * 0 frequency), to avoid degenerate trees. Note that optimal trees are not unique.
     * @return an optimal code tree for this frequency table
     */
    fun buildCodeTree(): CodeTree {
        // Note that if two nodes have the same frequency, then the tie is broken
        // by which tree contains the lowest symbol. Thus the algorithm has a
        // deterministic output and does not rely on the queue to break ties.
        val pqueue: Queue<NodeWithFrequency> = PriorityQueue()

        // Add leaves for symbols with non-zero frequency
        for (i in frequencies.indices) {
            if (frequencies[i] > 0) pqueue.add(NodeWithFrequency(Leaf(i), i, frequencies[i].toLong()))
        }

        // Pad with zero-frequency symbols until queue has at least 2 items
        var i = 0
        while (i < frequencies.size && pqueue.size < 2) {
            if (frequencies[i] == 0) pqueue.add(NodeWithFrequency(Leaf(i), i, 0))
            i++
        }
        if (pqueue.size < 2) throw AssertionError()

        // Repeatedly tie together two nodes with the lowest frequency
        while (pqueue.size > 1) {
            val x = pqueue.remove()
            val y = pqueue.remove()
            pqueue.add(
                NodeWithFrequency(
                    InternalNode(x.node, y.node),
                    min(x.lowestSymbol, y.lowestSymbol),
                    x.frequency + y.frequency
                )
            )
        }

        // Return the remaining node
        return CodeTree(pqueue.remove().node as InternalNode, frequencies.size)
    }

    // Helper structure for buildCodeTree()
    private class NodeWithFrequency(
        val node: Node,
        val lowestSymbol: Int, // Using wider type prevents overflow
        val frequency: Long
    ) : Comparable<NodeWithFrequency> {
        // Sort by ascending frequency, breaking ties by ascending symbol value.
        override fun compareTo(other: NodeWithFrequency): Int {
            return when {
                frequency < other.frequency -> -1
                frequency > other.frequency -> 1
                lowestSymbol < other.lowestSymbol -> -1
                lowestSymbol > other.lowestSymbol -> 1
                else -> 0
            }
        }
    }

    /**
     * Constructs a frequency table from the specified array of frequencies.
     * The array length must be at least 2, and each value must be non-negative.
     * @param freqs the array of frequencies
     * @throws NullPointerException if the array is `null`
     * @throws IllegalArgumentException if the array length is less than 2 or any value is negative
     */
    init {
        require(freqs.size >= 2) { "At least 2 symbols needed" }
        frequencies = freqs.clone() // Defensive copy
        for (x in frequencies) {
            require(x >= 0) { "Negative frequency" }
        }
    }
}