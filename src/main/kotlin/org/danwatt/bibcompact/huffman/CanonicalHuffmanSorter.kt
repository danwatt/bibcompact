package org.danwatt.bibcompact.huffman

import org.danwatt.bibcompact.huffman.canon2.Canonical_Huffman

object CanonicalHuffmanSorter {
    fun <T : Comparable<T>> sort(codes: Collection<Pair<T, InstanceOccurrences>>): List<T> {
        val bitMapping = buildBitMapping(emptyList(), codes)
        val c: Comparator<T> = compareBy<T> { bitMapping[it] }.thenComparing { it -> it }
        return codes.map { it.first }.sortedWith(c).toList()
    }

    fun <T> buildBitMapping(
        reservedAllocations: List<Int> = listOf(Integer.MAX_VALUE),
        incomingCodes: Collection<Pair<T, InstanceOccurrences>>
    ): Map<T, Int> {
        val codeFrequencies = IntArray(reservedAllocations.size + incomingCodes.size)
        reservedAllocations.forEachIndexed { index, i ->
            codeFrequencies[index] = i
        }
        incomingCodes.forEachIndexed { index, token ->
            codeFrequencies[index + reservedAllocations.size] = token.second
        }

        //val frequencies = FrequencyTable(codeFrequencies)
        //val originalCodeTree = frequencies.buildCodeTree()

        //val mapped = (incomingCodes.indices).map { c -> originalCodeTree.getCode(c).joinToString("") }

        val allCodes = if (reservedAllocations.size == 1) {
            listOf("" to Integer.MAX_VALUE)
        } else {
            emptyList()
        } + incomingCodes

        val lengths = Canonical_Huffman.testCanonicalHC(allCodes.size,
            allCodes.map { it.first.toString() }.toTypedArray(),
            allCodes.map { it.second }.toIntArray()
        )

        //val canonCode = CanonicalCode(originalCodeTree, frequencies.getSymbolLimit())
        val bitMapping = incomingCodes.mapIndexed { index, code ->
            code.first to lengths[index+reservedAllocations.size]/*canonCode.getCodeLength(index + reservedAllocations.size)*/
        }.toMap()

        println("Codes           : ${incomingCodes.map { it.first }.joinToString("\t")}")
        println("Code frequencies: ${codeFrequencies.joinToString("\t")}")
        //println("Code tree       : ${mapped.joinToString("\t")}")
        println("Keys            : ${bitMapping.keys.joinToString("\t")}")
        println("Values          : ${bitMapping.values.joinToString("\t")}")

        return bitMapping
    }
}

typealias InstanceOccurrences = Int