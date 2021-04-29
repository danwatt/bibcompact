package org.danwatt.bibcompact.huffman

import java.lang.AutoCloseable
import kotlin.Throws
import java.io.IOException
import java.lang.AssertionError
import java.io.EOFException
import java.io.InputStream
import java.util.*

/**
 * A stream of bits that can be read. Because they come from an underlying byte stream,
 * the total number of bits is always a multiple of 8. The bits are read in big endian.
 * Mutable and not thread-safe.
 * @see BitOutputStream
 */
class BitInputStream(val input: InputStream) : AutoCloseable {
    // Either in the range [0x00, 0xFF] if bits are available, or -1 if end of stream is reached.
    private var currentByte: Int

    // Number of remaining bits in the current byte, always between 0 and 7 (inclusive).
    private var numBitsRemaining: Int
    /*---- Methods ----*/
    /**
     * Reads a bit from this stream. Returns 0 or 1 if a bit is available, or -1 if
     * the end of stream is reached. The end of stream always occurs on a byte boundary.
     * @return the next bit of 0 or 1, or -1 for the end of stream
     * @throws IOException if an I/O exception occurred
     */
    @Throws(IOException::class)
    fun readBit(): Int {
        if (currentByte == -1) return -1
        if (numBitsRemaining == 0) {
            currentByte = input.read()
            if (currentByte == -1) return -1
            numBitsRemaining = 8
        }
        if (numBitsRemaining <= 0) throw AssertionError()
        numBitsRemaining--
        return currentByte ushr numBitsRemaining and 1
    }

    /**
     * Reads a bit from this stream. Returns 0 or 1 if a bit is available, or throws an `EOFException`
     * if the end of stream is reached. The end of stream always occurs on a byte boundary.
     * @return the next bit of 0 or 1
     * @throws IOException if an I/O exception occurred
     * @throws EOFException if the end of stream is reached
     */
    @Throws(IOException::class)
    fun readNoEof(): Int {
        val result = readBit()
        return if (result != -1) result else throw EOFException()
    }

    /**
     * Closes this stream and the underlying input stream.
     * @throws IOException if an I/O exception occurred
     */
    @Throws(IOException::class)
    override fun close() {
        input.close()
        currentByte = -1
        numBitsRemaining = 0
    }

    fun readBits(numBits: Int): Int {
        var n = 0
        for (i in (numBits - 1) downTo 0) {
            n = n or (this.readBit() shl i)
        }
        return n
    }

    fun finishByte() {
        while (numBitsRemaining > 0) {
            readBit()
        }
    }
    /*---- Constructor ----*/ /**
     * Constructs a bit input stream based on the specified byte input stream.
     * @param in the byte input stream
     * @throws NullPointerException if the input stream is `null`
     */
    init {
        currentByte = 0
        numBitsRemaining = 0
    }
}