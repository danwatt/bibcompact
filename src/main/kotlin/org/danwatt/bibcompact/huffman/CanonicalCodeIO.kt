package org.danwatt.bibcompact.huffman

import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max

object CanonicalCodeIO {
    fun write(code: CanonicalCode, out: BitOutputStream): Int {
        val bytesAtStart = out.bytesWritten
        val numCodes = code.getSymbolLimit()
        out.writeBits(numCodes, 16)
        val codeArray = IntArray(code.getSymbolLimit())
        var bitsNeededToWrite = 1
        for (i in 0 until code.getSymbolLimit()) {
            val v = code.getCodeLength(i)
            // For this file format, we only support codes up to 255 bits long
            if (v >= 256) throw RuntimeException("The code for a symbol is too long")
            codeArray[i] = v
            bitsNeededToWrite = max(bitsNeededToWrite, ceil(log2(v.toFloat())).toInt())
        }
        out.writeBits(bitsNeededToWrite, 8)
        var i = 0
        var run: Int

        while (i < codeArray.size) {
            out.writeBit(1)
            out.writeBits(codeArray[i], bitsNeededToWrite)
            run = 0
            while (i + 1 + run < codeArray.size
                && codeArray[i + 1 + run] == codeArray[i]
                && run < 255
            ) {
                run++
            }
            if (run > 2) {//TODO: dont hard-code 2
                out.writeBit(0)
                out.writeBits(run, 8)
                i += run
            }
            i++
        }
        out.finishByte()
        return out.bytesWritten - bytesAtStart
    }

    fun read(input: BitInputStream): CanonicalCode {
        val numCodes = input.readBits(16)
        val codeBits = input.readBits(8)
        var codeOffset = 0
        val codes = IntArray(numCodes)
        while (codeOffset < numCodes) {
            when (input.readBit()) {
                1 -> codes[codeOffset++] = input.readBits(codeBits)
                0 -> {
                    val runLength = input.readBits(8)
                    for (i in 0 until runLength) {
                        codes[codeOffset] = codes[codeOffset - 1]
                        codeOffset++
                    }
                }
            }
        }
        return CanonicalCode(codes)
    }
}