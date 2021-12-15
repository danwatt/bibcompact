package org.danwatt.bibcompact.trie

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class PrefixTrieReaderTest {
    val EOW = PrefixTrieWriter.WORD_MARKER
    val reader = PrefixTrieReader()

    @Test
    fun simple() {
        val encoded = listOf(
            'a', 'a', 'r', 'd', 'v', 'a', 'r', 'k', EOW,
            's', 5.c(),//BS, BS, BS, BS, BS
            'w', 'o', 'l', 'f', 5.c(),//BS, BS, BS, BS, BS,
            'o', 'n', 4.c(),//BS, BS, BS, BS,
            'b', 'a', 'c', 'k', 1.c(),//BS,
            'u', 's', 6.c(),//BS, BS, BS, BS, BS, BS,
            'b', 'o', 'b'
        )
        val wordList = sortedSetOf(
            "aardvark",
            "aardvarks",
            "aardwolf",
            "aaron",
            "aback",
            "abacus",
            "bob"
        )
        val output = reader.read(encoded)


        assertThat(output).containsExactlyElementsOf(wordList)
    }

    @Test
    fun suffixes() {
        val ES = (128 + 1).c()
        val ED = (128 + 2).c()
        val ING = (128 + 4).c()

        val encoded = listOf(
            'e', 's', (128).c(),
            'e', 'd', (128).c(),
            'i', 'n', 'g', (128).c(),
            'a', 'c', 't', ED, 3.c(),//4 BS to eliminate the a,c,t,(ed)
            'e', 'a', 't', ING, 3.c(),
            'l', 'u', 'n', 'c', 'h', ES
        )
        val wordList = sortedSetOf(
            "act",
            "acted",
            "eat",
            "eating",
            "lunch",
            "lunches",
        )

        val output = reader.read(encoded)


        assertThat(output).containsExactlyInAnyOrderElementsOf(wordList)
    }
}