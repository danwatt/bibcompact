package org.danwatt.bibcompact

import org.danwatt.bibcompact.huffman.*
import java.io.ByteArrayOutputStream
import java.util.*

fun writeHuffmanWithTree(
    tokens: List<Int>
): ByteArray {
    val byteOutput = ByteArrayOutputStream()
    val bitOutput = BitOutputStream(byteOutput)
    writeHuffmanWithTree(bitOutput, tokens)
    bitOutput.close()
    return byteOutput.toByteArray()
}

fun writeHuffmanWithTree(bitOutput: BitOutputStream, tokens: List<Int>) {
    val tokenFreqs = IntArray((tokens.maxOrNull() ?: 0) + 1)
    Arrays.fill(tokenFreqs, 0)
    tokens.forEach { tokenFreqs[it]++ }

    val codeTree = writeHuffmanHeader(tokenFreqs, bitOutput)
    val encoder = HuffmanEncoder(bitOutput, codeTree)
    tokens.forEach { encoder.write(it) }
    encoder.out.finishByte()
}

fun writeHuffmanHeader(
    initFreqs: IntArray,
    bitOutput: BitOutputStream
): CodeTree {
    val frequencies = FrequencyTable(initFreqs)
    val originalCodeTree = frequencies.buildCodeTree()
    val canonCode = CanonicalCode(originalCodeTree, frequencies.getSymbolLimit())

    CanonicalCodeIO.write(canonCode, bitOutput)

    return canonCode.toCodeTree()
}