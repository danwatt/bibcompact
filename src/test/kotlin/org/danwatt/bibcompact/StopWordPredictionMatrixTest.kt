package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StopWordPredictionMatrixTest {
    @Test
    fun test() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val stopWords = Version5Test.readStopwordFile()
        val tokenizer = VerseTokenizer()
        val tokenizedVerses = verses.map { tokenizer.tokenize(it).tokens }
        val versesWithJustStopWords =
            tokenizedVerses.map { val l = it.toMutableList(); l.retainAll(stopWords); l.toList() }

        val totalTokens = versesWithJustStopWords.map { it.size }.sum()
        assertThat(totalTokens).isEqualTo(493936)
        //>= 501 = 174848, or about 35% - 127 total
        //>= 200 = 254912 or about 50% - 387 total

        val g: Map<Pair<String, String>, Int> = versesWithJustStopWords.flatMap { verse ->
            verse.mapIndexedNotNull { index, token ->
                if (index > 0) {
                    verse[index - 1] to token
                } else {
                    null
                }
            }
        }.groupingBy {
            it
        }.eachCount().filterValues { it > 200 }
        g.forEach { (p, count) ->
            println("${p.first}\t${p.second}\t$count")
        }
        //Also run a model to see what the probability is for a transition from a stop to regular word

        //What might be important is compressing the RUN of predicitions

        /*
        the	of	22229
        the	and	6284
        of	the	12671

        the X of X the X and
        the -> of -> the -> and
        the -> 1st prediction -> 1st prediction -> 2nd prediction
         */

    }
}