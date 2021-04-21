package org.danwatt.bibcompact

data class LexiconEntry(
    val token: String,
    var firstVerse: Int,
    var lastVerse: Int = firstVerse,
    var totalOccurrences: Int = 0
) {
    fun addVerseInstance(verseIndex: Int) {
        totalOccurrences++
        if (verseIndex < firstVerse) firstVerse = verseIndex
        if (verseIndex > lastVerse) lastVerse = verseIndex
    }
}

class Lexicon(private val tokens: List<LexiconEntry>) {

    private var lookup: Map<String, Int> =
        tokens.mapIndexed { index, lexiconEntry -> lexiconEntry.token to index }
            .toMap()

    fun getTokens(): List<LexiconEntry> = this.tokens

    fun getLookupValue(token: String): Int? = lookup[token]
    fun getFullTokenStats(token: String): LexiconEntry? {
        return this.getTokens().firstOrNull() { it.token == token }
    }

    companion object {
        fun build(tokenized: List<TokenizedVerse>): Lexicon {
            val entries = mutableMapOf<String, LexiconEntry>()
            tokenized.forEachIndexed { idx, v ->
                v.tokens.forEach { token ->
                    val t = entries.computeIfAbsent(token) { LexiconEntry(token, idx) }
                    t.addVerseInstance(idx)
                }
            }
            val sorted = entries.values.sortedWith(
                compareByDescending<LexiconEntry> { it.totalOccurrences }
                    .thenBy { it.token }
            )
            return Lexicon(sorted)
        }
    }

}