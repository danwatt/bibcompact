package org.danwatt.bibcompact.huffman

import java.lang.AutoCloseable
import kotlin.Throws
import java.io.IOException
import java.io.OutputStream
import java.util.*

/*
 * Reference Huffman coding
 * Copyright (c) Project Nayuki
 *
 * https://www.nayuki.io/page/reference-huffman-coding
 * https://github.com/nayuki/Reference-Huffman-coding
 */ /**
 * A stream where bits can be written to. Because they are written to an underlying
 * byte stream, the end of the stream is padded with 0's up to a multiple of 8 bits.
 * The bits are written in big endian. Mutable and not thread-safe.
 * @see BitInputStream
 */
class BitOutputStream(val output: OutputStream) : AutoCloseable {
    // The accumulated bits for the current byte, always in the range [0x00, 0xFF].
    private var currentByte: Int

    // Number of accumulated bits in the current byte, always between 0 and 7 (inclusive).
    private var numBitsFilled: Int
    /*---- Methods ----*/
    /**
     * Writes a bit to the stream. The specified bit must be 0 or 1.
     * @param b the bit to write, which must be 0 or 1
     * @throws IOException if an I/O exception occurred
     */
    @Throws(IOException::class)
    fun write(b: Int) {
        require(!(b != 0 && b != 1)) { "Argument must be 0 or 1" }
        currentByte = currentByte shl 1 or b
        numBitsFilled++
        if (numBitsFilled == 8) {
            output.write(currentByte)
            currentByte = 0
            numBitsFilled = 0
        }
    }

    /**
     * Closes this stream and the underlying output stream. If called when this
     * bit stream is not at a byte boundary, then the minimum number of "0" bits
     * (between 0 and 7 of them) are written as padding to reach the next byte boundary.
     * @throws IOException if an I/O exception occurred
     */
    @Throws(IOException::class)
    override fun close() {
        while (numBitsFilled != 0) write(0)
        output.close()
    }
    /*---- Constructor ----*/ /**
     * Constructs a bit output stream based on the specified byte output stream.
     * @param out the byte output stream
     * @throws NullPointerException if the output stream is `null`
     */
    init {
        currentByte = 0
        numBitsFilled = 0
    }
}