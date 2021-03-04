package org.danwatt

data class Verse(
    val id: Int,
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val text: String
)

data class TokenizedVerse(
    val id: Int,
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val tokens: List<String>
)