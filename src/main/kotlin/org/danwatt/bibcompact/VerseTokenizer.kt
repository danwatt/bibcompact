package org.danwatt.bibcompact

import java.util.*

class VerseTokenizer {

    fun tokenize(verse: Verse): TokenizedVerse {
        val tokenizer = StringTokenizer(verse.text," .,;:'?!()",true)
        val tokens = tokenizer.asSequence().filter { it !=" " }.toList()
        return TokenizedVerse(verse.id, verse.book, verse.chapter, verse.verse, tokens as List<String>)
    }
}