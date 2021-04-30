package org.danwatt.bibcompact.futurework

import org.danwatt.bibcompact.Lexicon
import org.danwatt.bibcompact.TokenizedVerse
import org.danwatt.bibcompact.VerseStatsLexiconEntry
import java.util.*

class VerseCondenser(lexicon: Lexicon<VerseStatsLexiconEntry>) {
    fun apply(verse: TokenizedVerse): CondensedVerse {
        val tokens = 1
        return CondensedVerse(tokens, BitSet(1), IntArray(tokens), IntArray(1))
    }
}