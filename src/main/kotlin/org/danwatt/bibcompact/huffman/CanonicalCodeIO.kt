package org.danwatt.bibcompact.huffman

import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max

object CanonicalCodeIO {
    fun write(code: CanonicalCode, out: BitOutputStream): Int {
        println("=====Writing canonical code=====")
        //TODO: There is a lot of tuning that could happen in this file. Specifically, the maximum run of 255 is probably a bit too low
        /* Something is also off in generating the canonical code. The codes should be in sequentially increasing lengths
            but occasionally it will get shorter by one bit before going back up. This may be an issue in the underlying library
         */
        //In the end, this might save a few hundred bytes
        val bytesAtStart = out.bytesWritten
        val numCodes = code.getSymbolLimit()
        val codeLengths = IntArray(code.getSymbolLimit())
        var longestLength = 1
        for (i in 0 until code.getSymbolLimit()) {
            val length = code.getCodeLength(i)
            if (length >= 256) throw RuntimeException("The code for a symbol is too long")
            codeLengths[i] = length
            longestLength = max(longestLength, length)
        }
        //TODO: Handle more than 2^16 codes
        out.writeBits(numCodes, 16)
        val bitsNeededToWriteLength = ceil(log2(longestLength.toDouble()+1)).toInt()
        out.writeBits(bitsNeededToWriteLength, 8)


        println("There are ${numCodes} codes, the longest being ${longestLength}. This needs $bitsNeededToWriteLength bits to write")
        var i = 0
        var run: Int

        while (i < codeLengths.size) {
            out.writeBit(1)
            out.writeBits(codeLengths[i], bitsNeededToWriteLength)
            run = 0
            while (i + 1 + run < codeLengths.size
                && codeLengths[i + 1 + run] == codeLengths[i]
                && run < 255
            ) {
                run++
            }
            //We need to save at least 9 bits to make this worthwhile
            if (run * longestLength > 9) {
                out.writeBit(0)
                out.writeBits(run, 8)
                i += run
            }
            i++
        }
        out.finishByte()
        val bytesUsed = out.bytesWritten - bytesAtStart
        println("Canonical code consumed $bytesUsed bytes")
        println("Codes: ${codeLengths.joinToString(", ")}")
        return bytesUsed
    }

    fun read(input: BitInputStream): CanonicalCode {
        val numCodes = input.readBits(16)
        val codeBits = input.readBits(8)
        println("==== DECODING ====")
        println("There are $numCodes codes to read, each one will consume $codeBits bits")
        var codeOffset = 0
        val codeLengths = IntArray(numCodes)
        while (codeOffset < numCodes) {
            when (input.readBit()) {
                1 -> codeLengths[codeOffset++] = input.readBits(codeBits)
                0 -> {
                    val runLength = input.readBits(8)
                    for (i in 0 until runLength) {
                        codeLengths[codeOffset] = codeLengths[codeOffset - 1]
                        codeOffset++
                    }
                }
            }
        }
        println("Codes: ")
        println("Codes: ${codeLengths.joinToString(", ")}")
        input.finishByte()
        return CanonicalCode(codeLengths)
    }
}