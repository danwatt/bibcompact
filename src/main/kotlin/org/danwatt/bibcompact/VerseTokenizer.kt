package org.danwatt.bibcompact

import java.util.*

class VerseTokenizer {

    fun tokenize(verse: Verse): TokenizedVerse {
        val text = verse.text
        val tokens = tokenize(text)
        return TokenizedVerse(verse.id, verse.book, verse.chapter, verse.verse, tokens)
    }

    fun tokenize(text: String): List<String> {
        val tokenizer = StringTokenizer(text, " .,;:'?!()", true)
        return tokenizer.asSequence().filter { it != " " }.toList() as List<String>
    }
}