package org.danwatt.bibcompact.huffman

import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

object CanonicalCodeIO {
    fun write(code: CanonicalCode, out: BitOutputStream): Int {
        var bitsWritten = 0
        val numCodes = code.getSymbolLimit()
        for (i in 15 downTo 0) {
            out.write(numCodes ushr i and 1)
            bitsWritten++
        }
        val codeArray = IntArray(code.getSymbolLimit())
        var bitsNeededToWrite = 1
        for (i in 0 until code.getSymbolLimit()) {
            val v = code.getCodeLength(i)
            // For this file format, we only support codes up to 255 bits long
            if (v >= 256) throw RuntimeException("The code for a symbol is too long")
            codeArray[i] = v
            bitsNeededToWrite = max(bitsNeededToWrite, ceil(log2(v.toFloat())).toInt())
        }
        for (j in 7 downTo 0) {
            out.write(bitsNeededToWrite ushr j and 1)
            bitsWritten++
        }
        var i = 0
        var run = 0
        println("Using $bitsNeededToWrite bits for each codes")
        while (i < codeArray.size) {
            print("Code ${codeArray[i]}: 1")
            out.write(1)
            bitsWritten++
            for (j in (bitsNeededToWrite - 1) downTo 0) {
                print(codeArray[i] ushr j and 1)
                out.write(codeArray[i] ushr j and 1)
                bitsWritten++
            }
            println()
            run = 0
            while (i + 1 + run < codeArray.size
                && codeArray[i + 1 + run] == codeArray[i]
                && run < 255
            ) {
                run++
            }
            if (run > 2) {
                out.write(0)
                bitsWritten++
                print("Run of length $run: 0")
                for (j in 7 downTo 0) {
                    print(run ushr j and 1)
                    out.write(run ushr j and 1)
                    bitsWritten++
                }
                println()
                i += run
            }
            i++
        }
        while (bitsWritten % 8 != 0) {
            out.write(0)
            bitsWritten++
        }
        return bitsWritten / 8
    }

    fun read(input: BitInputStream): CanonicalCode {
        val a = IntArray(123)
        return CanonicalCode(a)
    }
}