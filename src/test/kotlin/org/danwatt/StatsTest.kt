package org.danwatt

import org.junit.Test

class StatsTest {
    @Test
    fun outputSomeStats() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val top20 = verses.sortedByDescending { it.text.length }.take(20)
        top20.forEach {
            println("${it.id} (${it.text.length}): ${it.text}")
        }

        val t = VerseTokenizer()

        val tokenized = verses.map { t.tokenize(it) }
        val top20Tokenized = tokenized.sortedByDescending { it.tokens.size }.take(20)
        top20Tokenized.forEach {
            println("${it.id} (${it.tokens.size} / ${it.tokens.distinct().size}): ${it.tokens}")
        }
    }
}