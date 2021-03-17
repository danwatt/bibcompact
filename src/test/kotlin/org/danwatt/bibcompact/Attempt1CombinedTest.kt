package org.danwatt.bibcompact

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.FileOutputStream

class Attempt1CombinedTest {
    @Test
    fun kjvTest() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val t = VerseTokenizer()
        val tokenized = verses.map { t.tokenize(it) }
        val lex = Lexicon.build(tokenized)
        println("There are ${lex.getTokens().size} tokens")

        val writer = TokenizedVerseWriter(lex)
        val verseBytes = tokenized.flatMap {
            writer.write(it).asList()
        }

        val lexBytes = LexiconWriter().write(lex)
        assertThat(verseBytes).hasSize(1236508)
        assertThat(lexBytes).hasSize(109035)

        writer.outputStats()

        val fw = FileOutputStream("/tmp/kjv.out")
        fw.write(lexBytes)
        fw.write(verseBytes.toByteArray())
        fw.close()
    }
}