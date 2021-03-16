package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat
import org.danwatt.bibcompact.Verse
import org.danwatt.bibcompact.VerseTokenizer
import org.junit.Test

class VerseTokenizerTest {

    @Test
    fun tokenizeVerse() {
        val testVerse = Verse(1, 1, 1, 1, "This, is a test. This is: a test! Test-ing?")
        val result = VerseTokenizer().tokenize(testVerse)
        assertThat(result.tokens).containsSequence(
            "This",
            ",",
            "is",
            "a",
            "test",
            ".",
            "This",
            "is",
            ":",
            "a",
            "test",
            "!",
            "Test-ing",
            "?"
        )
    }
}