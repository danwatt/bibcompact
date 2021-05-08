package org.danwatt.bibcompact

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
    var tokens: List<String>
)