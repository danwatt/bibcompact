package org.danwatt.bibcompact


import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LexiconTest {
    @Test
    fun kjv() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val tokenized = verses.map { VerseTokenizer().tokenize(it) }
        val dict = Lexicon.build(tokenized)

        val sorted = dict.getTokens().sortedByDescending { it.totalOccurrences }
        assertThat(sorted[0].token).isEqualTo(",")
        assertThat(sorted[0].totalOccurrences).isEqualTo(70574)
        assertThat(sorted[0].firstVerse).isEqualTo(1)
        assertThat(sorted[0].lastVerse).isEqualTo(31101)
        val f = (0xFF).toByte()
    }

    @Test
    fun smallerTest() {
        val d = Lexicon.build(
            listOf(
                TokenizedVerse(1, 1, 1, 1, listOf("Test")),
                TokenizedVerse(2, 1, 1, 2, listOf("Test", "value")),
                TokenizedVerse(3, 1, 1, 3, listOf("Test", "something", "else"))
            )
        )

        assertThat(d.getTokens().map { it.token }).containsSequence("Test", "else", "something", "value")
        assertThat(d.getTokens()[0].firstVerse).isEqualTo(0)
        assertThat(d.getTokens()[0].lastVerse).isEqualTo(2)

        assertThat(d.getLookupValue("Test")).isEqualTo(0)
        assertThat(d.getLookupValue("else")).isEqualTo(1)
        assertThat(d.getLookupValue("something")).isEqualTo(2)
        assertThat(d.getLookupValue("value")).isEqualTo(3)
    }
}