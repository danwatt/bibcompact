package org.danwatt.bibcompact.compression

import org.danwatt.bibcompact.writeHuffmanWithTree

class BigramCompressor {
    fun compressBigrams(codeList: List<Int>, maxPairsToKeep: Int = 128): List<Int> {
        val top = codeList.filter { it > 0 }
            .zipWithNext { a, b -> a to b }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(maxPairsToKeep)
        val ngrams = top.map { it.first }
        val mapped = ngrams.mapIndexed { index, pair -> pair to (index) }.toMap()

        var i = 0
        val out = mutableListOf<Int>()
        while (i < codeList.size) {
            val currentCode = codeList[i]
            if (i < codeList.size - 1) {
                val nextCode = codeList[i + 1]
                val ng = currentCode to nextCode
                val m: Int? = mapped[ng]
                if (m != null) {
                    out.add(m)
                    i++
                } else {
                    out.add(maxPairsToKeep + currentCode)
                }
            } else {
                out.add(maxPairsToKeep + currentCode)
            }
            i++
        }
        val ngramCodes = listOf(maxPairsToKeep) + mapped.asSequence().sortedBy { it.value }
            .flatMap { listOf(it.key.first, it.key.second) }.toList()

        val finalOutput = ngramCodes + out
        val compressed = writeHuffmanWithTree(finalOutput)
        val originalCompressed = writeHuffmanWithTree(codeList)
        println("BiGram: ${maxPairsToKeep}. Original: ${codeList.size} original compressed: ${originalCompressed.size}, n-gram uncompressed: ${finalOutput.size}, n-gram compressed: ${compressed.size}. Savings: ${originalCompressed.size - compressed.size}")
        return finalOutput
    }
}