package org.danwatt.bibcompact.huffman

import org.junit.Assert
import org.junit.Test
import java.io.EOFException
import java.io.IOException
import java.util.*

/*
 * Reference Huffman coding
 * Copyright (c) Project Nayuki
 *
 * https://www.nayuki.io/page/reference-huffman-coding
 * https://github.com/nayuki/Reference-Huffman-coding
 */ /**
 * Tests the compression and decompression of a complete Huffman coding application, using the JUnit test framework.
 */
abstract class HuffmanCodingTest {
    /* Test cases */
    @Test
    fun testEmpty() {
        test(ByteArray(0))
    }

    @Test
    fun testOneSymbol() {
        test(ByteArray(10))
    }

    @Test
    fun testSimple() {
        test(byteArrayOf(0, 3, 1, 2))
    }

    @Test
    fun testEveryByteValue() {
        val b = ByteArray(256)
        for (i in b.indices) b[i] = i.toByte()
        test(b)
    }

    @Test
    fun testFibonacciFrequencies() {
        val b = ByteArray(87)
        var i = 0
        run {
            var j = 0
            while (j < 1) {
                b[i] = 0
                j++
                i++
            }
        }
        run {
            var j = 0
            while (j < 2) {
                b[i] = 1
                j++
                i++
            }
        }
        run {
            var j = 0
            while (j < 3) {
                b[i] = 2
                j++
                i++
            }
        }
        run {
            var j = 0
            while (j < 5) {
                b[i] = 3
                j++
                i++
            }
        }
        run {
            var j = 0
            while (j < 8) {
                b[i] = 4
                j++
                i++
            }
        }
        run {
            var j = 0
            while (j < 13) {
                b[i] = 5
                j++
                i++
            }
        }
        run {
            var j = 0
            while (j < 21) {
                b[i] = 6
                j++
                i++
            }
        }
        var j = 0
        while (j < 34) {
            b[i] = 7
            j++
            i++
        }
        test(b)
    }

    @Test
    fun testRandomShort() {
        for (i in 0..99) {
            val b = ByteArray(random.nextInt(1000))
            random.nextBytes(b)
            test(b)
        }
    }

    @Test
    fun testRandomLong() {
        for (i in 0..2) {
            val b = ByteArray(random.nextInt(1000000))
            random.nextBytes(b)
            test(b)
        }
    }

    /* Utilities */ // Tests that the given byte array can be compressed and decompressed to the same data, and not throw any exceptions.
    private fun test(b: ByteArray) {
        try {
            val compressed = compress(b)
            val decompressed = decompress(compressed)
            Assert.assertArrayEquals(b, decompressed)
        } catch (e: EOFException) {
            Assert.fail("Unexpected EOF")
        } catch (e: IOException) {
            throw AssertionError(e)
        }
    }

    /* Abstract methods */ // Compression method that needs to be supplied by a subclass.
    protected abstract fun compress(b: ByteArray): ByteArray

    // Decompression method that needs to be supplied by a subclass.
    protected abstract fun decompress(b: ByteArray): ByteArray

    companion object {
        private val random = Random()
    }
}