package org.danwatt.bibcompact

data class LexiconEntry(
    val token: String,
    var firstVerse: Int,
    var lastVerse: Int = firstVerse,
    var totalOccurrences: Int = 0
) {
    fun addVerseInstance(verseIndex: Int, book: Int) {
        totalOccurrences++
        if (verseIndex < firstVerse) firstVerse = verseIndex
        if (verseIndex > lastVerse) lastVerse = verseIndex
    }
}

class Lexicon {

    private var lookup: Map<String, Int>
    private val tokens: List<LexiconEntry>

    constructor(tokens: List<LexiconEntry>) {
        this.lookup = tokens.mapIndexed { index, lexiconEntry -> lexiconEntry.token to index }.toMap()
        this.tokens = tokens
    }

    fun getTokens(): List<LexiconEntry> {
        return this.tokens
    }

    fun offset(token: String): Int? {
        return lookup[token]
    }

    companion object {
        fun build(tokenized: List<TokenizedVerse>): Lexicon {
            val entries = mutableMapOf<String, LexiconEntry>()
            tokenized.forEachIndexed { idx, v ->
                v.tokens.forEach { token ->
                    val t = entries.computeIfAbsent(token) { LexiconEntry(token, idx) }
                    t.addVerseInstance(idx, v.book)
                }
            }
            val sorted =
                entries.values.sortedWith(compareByDescending <LexiconEntry> { it.totalOccurrences }.thenBy { it.token })
            return Lexicon(sorted)
        }
    }

}