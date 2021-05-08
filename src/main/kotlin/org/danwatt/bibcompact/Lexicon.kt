package org.danwatt.bibcompact

sealed class BaseLexiconEntry(open val token: String)
data class TokenOnlyEntry(override val token: String) : BaseLexiconEntry(token = token)

data class VerseStatsLexiconEntry(
    override val token: String,
    var firstVerse: Int? = null,
    var lastVerse: Int? = firstVerse,
    var totalOccurrences: Int = 0
) : BaseLexiconEntry(token) {
    fun addVerseInstance(verseIndex: Int) {
        totalOccurrences++
        if (firstVerse == null) {
            firstVerse = verseIndex
        } else {
            if (verseIndex < firstVerse!!) firstVerse = verseIndex
        }
        if (lastVerse == null) {
            lastVerse = verseIndex
        } else {
            if (verseIndex > lastVerse!!) lastVerse = verseIndex
        }

    }
}

class Lexicon<T : BaseLexiconEntry>(private val tokens: List<T>) {

    private var lookup: Map<String, Int> =
        tokens.mapIndexed { index, lexiconEntry -> lexiconEntry.token to index }
            .toMap()

    fun getTokens(): List<T> = this.tokens

    fun getLookupValue(token: String): Int? = lookup[token]
    fun getFullTokenStats(token: String): T? {
        return this.getTokens().firstOrNull { it.token == token }
    }

    companion object {
        fun build(tokenized: List<TokenizedVerse>): Lexicon<VerseStatsLexiconEntry> {
            val entries = mutableMapOf<String, VerseStatsLexiconEntry>()
            tokenized.forEachIndexed { idx, v ->
                v.tokens.forEach { token ->
                    val t = entries.computeIfAbsent(token) { VerseStatsLexiconEntry(token, idx) }
                    t.addVerseInstance(idx)
                }
            }
            val sorted = entries.values.sortedWith(
                compareByDescending<VerseStatsLexiconEntry> { it.totalOccurrences }
                    .thenBy { it.token }
            )
            return Lexicon(sorted)
        }

        fun buildFromWordList(words: List<String>): Lexicon<TokenOnlyEntry> {
            return Lexicon(words.map { TokenOnlyEntry(it) })
        }
    }

}
/*
Bit distribtuon 10: 75
Bit distribtuon 11: 137
Bit distribtuon 12: 237
Bit distribtuon 13: 348
Bit distribtuon 14: 623  | 12,708
Bit distribtuon 15: 939  | 12,085
Bit distribtuon 16: 1305 | 11,146
Bit distribtuon 17: 1840 | 9,841
Bit distribtuon 18: 1723 | 8,001
Bit distribtuon 19: 1886 | 6,278
Bit distribtuon 20: 4392
2^13 = 8192

 */