package org.danwatt.bibcompact.trie

import org.assertj.core.api.Assertions.assertThat
import org.danwatt.bibcompact.BibleCsvParser
import org.danwatt.bibcompact.VerseTokenizer
import org.junit.Test

class TrieIntegrationTest {
    val verses = BibleCsvParser().readTranslation("kjv")
    val tokenizer = VerseTokenizer()
    val tokenized = verses.map { tokenizer.tokenize(it) }.toList()
    val distinctWords = tokenized.flatMap { it.tokens }.distinct().toSortedSet()

    @Test
    fun kvjThereAndBackWithSuffixes() {
        val encoded =
            PrefixTrieWriter().write(distinctWords, listOf("s", "ed", "ing", "eth", "th", "h", "d"))
        val decodedList = PrefixTrieReader().read(encoded)

        assertThat(decodedList).containsExactlyInAnyOrderElementsOf(distinctWords)
    }

    @Test
    fun kvjThereAndBackWithoutSuffixes() {
        val encoded = PrefixTrieWriter().write(distinctWords)
        val decodedList = PrefixTrieReader().read(encoded)

        assertThat(decodedList).containsExactlyInAnyOrderElementsOf(distinctWords)
    }
}