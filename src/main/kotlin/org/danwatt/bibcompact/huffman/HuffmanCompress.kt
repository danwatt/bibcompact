package org.danwatt.bibcompact.huffman

import java.io.*

/*
 * Reference Huffman coding
 * Copyright (c) Project Nayuki
 *
 * https://www.nayuki.io/page/reference-huffman-coding
 * https://github.com/nayuki/Reference-Huffman-coding
 */ /**
 * Compression application using static Huffman coding.
 *
 * Usage: java HuffmanCompress InputFile OutputFile
 *
 * Then use the corresponding "HuffmanDecompress" application to recreate the original input file.
 *
 * Note that the application uses an alphabet of 257 symbols - 256 symbols for the byte values
 * and 1 symbol for the EOF marker. The compressed file format starts with a list of 257
 * code lengths, treated as a canonical code, and then followed by the Huffman-coded data.
 */
object HuffmanCompress {
    // Command line main application function.
    @JvmStatic
    fun main(args: Array<String>) {
        // Handle command line arguments
        if (args.size != 2) {
            System.err.println("Usage: java HuffmanCompress InputFile OutputFile")
            System.exit(1)
            return
        }
        val inputFile = File(args[0])
        val outputFile = File(args[1])

        // Read input file once to compute symbol frequencies.
        // The resulting generated code is optimal for static Huffman coding and also canonical.
        val freqs = getFrequencies(inputFile)
        freqs.increment(256) // EOF symbol gets a frequency of 1
        var code = freqs.buildCodeTree()
        val canonCode = CanonicalCode(code, freqs.getSymbolLimit())
        // Replace code tree with canonical one. For each symbol,
        // the code value may change but the code length stays the same.
        code = canonCode.toCodeTree()
        BufferedInputStream(FileInputStream(inputFile)).use { `in` ->
            val out = BitOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))
            writeCodeLengthTable(out, canonCode)
            compress(code, `in`, out)

        }
    }

    // Returns a frequency table based on the bytes in the given file.
    // Also contains an extra entry for symbol 256, whose frequency is set to 0.
    @Throws(IOException::class)
    private fun getFrequencies(file: File): FrequencyTable {
        val freqs = FrequencyTable(IntArray(257))
        BufferedInputStream(FileInputStream(file)).use { input ->
            while (true) {
                val b = input.read()
                if (b == -1) break
                freqs.increment(b)
            }
        }
        return freqs
    }

    // To allow unit testing, this method is package-private instead of private.
    @Throws(IOException::class)
    fun writeCodeLengthTable(out: BitOutputStream, canonCode: CanonicalCode) {
        for (i in 0 until canonCode.getSymbolLimit()) {
            val v = canonCode.getCodeLength(i)
            // For this file format, we only support codes up to 255 bits long
            if (v >= 256) throw RuntimeException("The code for a symbol is too long")

            // Write value as 8 bits in big endian
            for (j in 7 downTo 0) out.write(v ushr j and 1)
        }
    }

    // To allow unit testing, this method is package-private instead of private.
    @Throws(IOException::class)
    fun compress(code: CodeTree?, `in`: InputStream, out: BitOutputStream?) {
        val enc = HuffmanEncoder(out!!)
        enc.codeTree = code
        while (true) {
            val b = `in`.read()
            if (b == -1) break
            enc.write(b)
        }
        enc.write(256) // EOF
    }
}