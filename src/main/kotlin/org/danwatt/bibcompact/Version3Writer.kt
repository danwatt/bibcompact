package org.danwatt.bibcompact

class Version3Writer : BibWriter(3) {
    override fun writeVerseData(tokenized: List<TokenizedVerse>, lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {
        //This likely will be a copy of version 2
        TODO("Not yet implemented")
    }

    override fun writeLexicon(lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {
        //Write the lexicon as a trie
        //This might necessitate reorganizing the lexicon, so some interface changes might be necessary
        TODO("Not yet implemented")
    }
}