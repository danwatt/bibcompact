package org.danwatt.bibcompact.huffman

import kotlin.math.max

object CanonicalCodeIO {
    fun write(code: CanonicalCode, out: BitOutputStream): Int {
        val bytesAtStart = out.bytesWritten
        val numCodes = code.getSymbolLimit()
        val codeLengths = IntArray(code.getSymbolLimit())
        var bitsNeededToWrite = 1
        for (i in 0 until code.getSymbolLimit()) {
            val length = code.getCodeLength(i)
            if (length >= 256) throw RuntimeException("The code for a symbol is too long")
            codeLengths[i] = length
            bitsNeededToWrite = max(bitsNeededToWrite, length)
        }
        out.writeBits(numCodes, 16)
        out.writeBits(bitsNeededToWrite, 8)

        var i = 0
        var run: Int

        while (i < codeLengths.size) {
            out.writeBit(1)
            out.writeBits(codeLengths[i], bitsNeededToWrite)
            run = 0
            while (i + 1 + run < codeLengths.size
                && codeLengths[i + 1 + run] == codeLengths[i]
                && run < 255
            ) {
                run++
            }
            //We need to save at least 9 bits to make this worthwhile
            if (run * bitsNeededToWrite > 9) {
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
        input.finishByte()
        return CanonicalCode(codes)
    }
}