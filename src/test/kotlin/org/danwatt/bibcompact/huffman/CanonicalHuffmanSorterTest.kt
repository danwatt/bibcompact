package org.danwatt.bibcompact.huffman

import org.assertj.core.api.Assertions.assertThat
import org.danwatt.bibcompact.BibleCsvParser
import org.danwatt.bibcompact.Lexicon
import org.danwatt.bibcompact.VerseTokenizer
import org.danwatt.bibcompact.writeHuffmanWithTree
import org.junit.Test

class CanonicalHuffmanSorterTest {
    @Test
    fun test1() {
        val sorted = CanonicalHuffmanSorter.sort(listOf("A" to 5, "B" to 1, "C" to 6, "D" to 10))
        assertThat(sorted).containsExactly("D", "C", "A", "B")
    }

    @Test
    fun test2() {
        val tokenizer = VerseTokenizer()

        val verses = BibleCsvParser().readTranslation("kjv")
            .filter { it.book == 1 && it.chapter == 1 }
            .map { tokenizer.tokenize(it) }

        val lex = Lexicon.build(verses)
        val freqs = lex.getTokens().map { it.token to it.totalOccurrences }

        val sorted: Map<String, Int> = CanonicalHuffmanSorter.buildBitMapping(emptyList(), freqs)
        //writeHuffmanWithTree(sorted)
    }
}