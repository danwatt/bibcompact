package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StatsTest {
    val verses = BibleCsvParser().readTranslation("kjv")
    val t = VerseTokenizer()
    val tokenized = verses.map { t.tokenize(it) }

    @Test
    fun `book chapter verse counts`() {
        val books = verses.map { it.book }.distinct().count()
        val chapter = verses.map { it.book to it.chapter }.distinct().count()
        val verse = verses.size

        assertThat(books).isEqualTo(66)
        assertThat(chapter).isEqualTo(1189)
        assertThat(verse).isEqualTo(31103)
    }

    @Test
    fun top20VerseLength() {
        val top20 = verses.sortedByDescending { it.text.length }.take(20)
        top20.forEach {
            println("${it.id} (${it.text.length}): ${it.text}")
        }
        assertThat(top20[0].id).isEqualTo(17008009)
    }

    @Test
    fun top20TokenizedVerseLength() {
        val top20Tokenized = tokenized.sortedByDescending { it.tokens.size }.take(20)
        top20Tokenized.forEach {
            println("${it.id} (${it.tokens.size} / ${it.tokens.distinct().size}): ${it.tokens}")
        }

        assertThat(top20Tokenized[0].id).isEqualTo(17008009)
        assertThat(top20Tokenized[0].tokens).hasSize(104)
    }

    @Test
    fun tokenStats() {
        val allTokens = tokenized.flatMap { it.tokens }.toList()
        assertThat(allTokens).hasSize(917089)
        assertThat(allTokens.distinct()).hasSize(13600)

        val counts = allTokens.groupingBy { it }.eachCount()
        val over1000: Map<String, Int> = counts.filterValues { it > 1000 }
        assertThat(over1000).hasSize(116)

        over1000.map { (k, v) -> v to k }.sortedByDescending { it.first }.forEach { println(it) }

        val sortedBySize = counts.toList().sortedByDescending { it.second }
        val top = sortedBySize.take(500)
        assertThat(top).containsSequence(
            "," to 70574,
            "the" to 62064,
            "and" to 38847
        )
        top.forEachIndexed { index, pair ->
            println("${pair.first}\t${pair.second}")
        }

        val bytesNeeded = tokenized.map { verse ->
            (verse.tokens.size+8-1) / 8
        }.sum()
        println("${bytesNeeded} bytes are needed for bit mapping")
    }
}