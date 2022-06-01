package org.danwatt

import org.danwatt.bibcompact.trie.PrefixTrieWriter
import java.nio.charset.Charset
import java.util.*

data class Block(val letters: MutableSet<Char> = mutableSetOf()) {
    fun contains(char: Char) = letters.contains(char)
    fun hasRoom() = letters.size < 6
    fun isEmpty() = letters.isEmpty()
    fun add(char: Char) {
        if (!hasRoom() && !letters.contains(char)) {
            throw IllegalArgumentException()
        }
        letters.add(char)
    }
}

fun main() {
    val maxWordLength = 7

    val words = PrefixTrieWriter::class.java.getResourceAsStream("/en_words.txt")
        .bufferedReader(Charset.forName("UTF-8"))
        .use { it.readLines() }.distinct()
        .map { it.lowercase() }
        .filter { it.length <= maxWordLength }
        .toSortedSet()
    val shortWords = words.filter { it.length <= 3 }
    val longWords = words.filter { it.length > 3 }
    println("There are ${shortWords.size} short words, ${longWords.size} long words")
    val m = buildLetterPairDistributions(words)

    m.forEach { (p,c)->
        println("$p: $c")
    }

    ////J moved to E, Q moved to O: 671 words
    //O moved to E, L & C moved to T,
    //EARIO 10, TNSLC 10, UDPM 7, HGBF 6, YWKV 5, XZJQ 4: 248
    //EACIO 10, TNSLR 10, UDPM 7, HGBF 6, YWKV 5, XZJQ 4: 218
    //EACI 9, TNSLR 10, UDPMO 8, HGBF 6, YWKV 5, XZJQ 4: 175
    //EACI 10, TNSLR 10, UDPMO 8, HGBF 6, YWKV 5, XZJQ 4: 115
    val b = listOf(
        "eaci" to 10,
        "tnslr" to 10,
        "udpmo" to 8,
        "hgbf" to 6,
        "ywkv" to 5,
        "xzjq" to 4
    )

    val blocks = b.map { p->
        val face = p.first.map { it }.toList()
        val count = p.second
        (1..count).map { Block(face.toMutableSet()) }
    }.flatten()

    testViability(blocks, words)
    println("There are ${blocks.size} blocks")


}

fun testViability(blocks: List<Block>, words: SortedSet<String>) {
    val letterMapping = mutableMapOf<Char, Char>()
    val blockWeights = mutableMapOf<Char, Int>()

    blocks.forEach { block ->
        block.letters.forEach { letter ->
            letterMapping.put(letter, block.letters.first())
        }
        blockWeights[block.letters.first()] = blockWeights.getOrDefault(block.letters.first(), 0) + 1
    }

    println("Mapping: $letterMapping")
    println("Weights: $blockWeights")

    var missingWords = 0
    val wordsAsList = words.toList()
    wordsAsList.forEach { word ->
        val mappedToFirstLetter: Map<Char, List<Char>> = word.map { letterMapping[it]!! }.groupBy { it }
        mappedToFirstLetter.forEach { firstLetter, listCount ->
            val actualCost = listCount.size * 2
            if (blockWeights[firstLetter]!! < actualCost) {
                println("$word (${listCount.size *2} '$firstLetter' blocks, available is ${blockWeights[firstLetter]})")
                missingWords++
            }
        }
    }
    println("That means $missingWords could not be used twice")
}


private fun buildLetterPairDistributions(words: Set<String>): MutableMap<Pair<Char, Char>, Int> {
    val m = mutableMapOf<Pair<Char, Char>, Int>()
    for (a in ('a'..'z')) {
        for (b in a..'z') {
            m[a to b] = 0
        }
    }
    words.forEach { word ->
        word.forEachIndexed { index, c ->
            if (index > 0) {
                val p = word[index - 1]
                val pair = if (c < p) {
                    c to p
                } else {
                    p to c
                }
                m[pair] = m.getOrDefault(pair, 0) + 1
            }
        }
    }
    return m
}