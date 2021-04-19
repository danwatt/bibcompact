package org.danwatt.bibcompact.experiments

import org.danwatt.bibcompact.BibleCsvParser
import org.danwatt.bibcompact.TokenizedVerse
import org.danwatt.bibcompact.VerseTokenizer
import org.junit.Test

class NGramTests {
    private val verses = BibleCsvParser().readTranslation("kjv")
    private val t = VerseTokenizer()
    private val tokenized: List<TokenizedVerse> = verses.map { t.tokenize(it) }

    @Test
    fun digrams() {
        tokenized.flatMap {
            (0 until it.tokens.size - 1).map { index ->
                it.tokens[index] to it.tokens[index + 1]
            }
        }.toList()
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(128).forEach {
                println("${it.first} is used ${it.second}")
            }
    }

    @Test
    fun trigrams() {
        tokenized.flatMap {
            (0 until it.tokens.size - 2).map { index ->
                Triple(it.tokens[index], it.tokens[index + 1], it.tokens[index + 2])
            }
        }.toList()
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(128).forEach {
                println("${it.first} is used ${it.second}")
            }
    }
}