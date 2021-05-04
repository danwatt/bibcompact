package org.danwatt.bibcompact.huffman

import java.io.*
import java.util.*
import kotlin.system.exitProcess

/**
 * Compression application using adaptive Huffman coding.
 *
 * Usage: java AdaptiveHuffmanCompress InputFile OutputFile
 *
 * Then use the corresponding "AdaptiveHuffmanDecompress" application to recreate the original input file.
 *
 * Note that the application starts with a flat frequency table of 257 symbols (all set to a frequency of 1),
 * collects statistics while bytes are being encoded, and regenerates the Huffman code periodically. The
 * corresponding decompressor program also starts with a flat frequency table, updates it while bytes are being
 * decoded, and regenerates the Huffman code periodically at the exact same points in time. It is by design that
 * the compressor and decompressor have synchronized states, so that the data can be decompressed properly.
 */
object AdaptiveHuffmanCompress {
    // Command line main application function.
    @JvmStatic
    fun main(args: Array<String>) {
        // Handle command line arguments
        if (args.size != 2) {
            System.err.println("Usage: java AdaptiveHuffmanCompress InputFile OutputFile")
            exitProcess(1)
            return
        }
        val inputFile = File(args[0])
        val outputFile = File(args[1])
        BufferedInputStream(FileInputStream(inputFile)).use {
            val out = BitOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))
            compress(it, out)
        }
    }

    // To allow unit testing, this method is package-private instead of private.
    fun compress(input: InputStream, out: BitOutputStream) {
        val initFreqs = IntArray(257)
        Arrays.fill(initFreqs, 1)
        var freqs = FrequencyTable(initFreqs)
        // Don't need to make canonical code because we don't transmit the code tree
        val enc = HuffmanEncoder(out,freqs.buildCodeTree())
        var count = 0 // Number of bytes read from the input file
        while (true) {
            // Read and encode one byte
            val symbol = input.read()
            if (symbol == -1) break
            enc.write(symbol)
            count++

            // Update the frequency table and possibly the code tree
            freqs.increment(symbol)
            if (count < 262144 && isPowerOf2(count) || count % 262144 == 0) // Update code tree
                enc.codeTree = freqs.buildCodeTree()
            if (count % 262144 == 0) // Reset frequency table
                freqs = FrequencyTable(initFreqs)
        }
        enc.write(256) // EOF
    }


    fun compress(input: List<Int>,maxCode: Int, out: BitOutputStream) {
        val initFreqs = IntArray(maxCode+1)
        Arrays.fill(initFreqs, 1)
        var freqs = FrequencyTable(initFreqs)
        // Don't need to make canonical code because we don't transmit the code tree
        val enc = HuffmanEncoder(out,freqs.buildCodeTree())
        var count = 0 // Number of bytes read from the input file

        var i = 0
        while (i < input.size) {
            // Read and encode one byte
            val symbol = input[i++]
            enc.write(symbol)
            count++

            // Update the frequency table and possibly the code tree
            freqs.increment(symbol)
            if (count < 262144 && isPowerOf2(count) || count % 262144 == 0) // Update code tree
                enc.codeTree = freqs.buildCodeTree()
            if (count % 262144 == 0) // Reset frequency table
                freqs = FrequencyTable(initFreqs)
        }
    }

    private fun isPowerOf2(x: Int): Boolean = x > 0 && Integer.bitCount(x) == 1
}