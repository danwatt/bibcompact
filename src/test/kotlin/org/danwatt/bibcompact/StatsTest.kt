package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.FileWriter
import java.io.PrintWriter
import kotlin.math.ceil
import kotlin.math.log2

class StatsTest {
    private val verses = BibleCsvParser().readTranslation("kjv")
    private val t = VerseTokenizer()
    private val tokenized = verses.map { t.tokenize(it) }

    @Test
    fun `book chapter verse counts`() {
        val books = verses.map { it.book }.distinct().count()
        val chapter = verses.map { it.book to it.chapter }.distinct().count()
        val verse = verses.size

        assertThat(books).isEqualTo(66)
        assertThat(chapter).isEqualTo(1189)
        assertThat(verse).isEqualTo(31103)
    }

    @Test
    fun dumpForSQL() {
        val out = PrintWriter(FileWriter("/tmp/verses.csv"))
        //out.println("Book|Chapter|Verse|Token|ChapterOffset|VerseOffset")
        var previousVerse: TokenizedVerse? = null
        var chapterOffset = 0
        tokenized.forEach { verse ->
            if (previousVerse == null || (previousVerse!!.book != verse.verse && previousVerse!!.chapter != verse.chapter && verse.verse == 1)) {
                chapterOffset = 0
            }
            previousVerse = verse
            verse.tokens.forEachIndexed { verseOffset, token ->
                val type = when {
                    token.matches(Regex("^\\w+$")) -> 1
                    else -> 0
                }
                out.println(verse.book.toString() + "|" + verse.chapter + "|" + verse.verse + "|" + token + "|" + type + "|" + chapterOffset + "|" + verseOffset)
                chapterOffset++
            }

        }
        out.close()

    }

    @Test
    fun firstWords() {
        //Count how many words appear capitalized,
        //then compare how many of them are only capitalized at the start of a sentence.
        val wordsFollowingPunctuation = mutableListOf<String>()
        val wordsNotFollowingPunctuation = mutableListOf<String>()
        val punctuation = setOf("?", "!", ".")
        tokenized.forEach { verse ->
            var followingPunctuation = true
            verse.tokens.forEach { token ->
                followingPunctuation = if (punctuation.contains(token)) {
                    true
                } else {
                    if (followingPunctuation) {
                        if (token.toLowerCase() == token) {
                            println("Word $token is after punctuation yet is lower case, in verse: ${verse.id}")
                        }
                        wordsFollowingPunctuation.add(token)
                    } else {
                        wordsNotFollowingPunctuation.add(token)
                    }
                    false
                }
            }
        }
        val top =
            wordsFollowingPunctuation.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }

        top.forEach { (k, count) -> println("$k: $count") }


    }

    @Test
    fun top20VerseLength() {
        val top20 = verses.sortedByDescending { it.text.length }.take(20)
        top20.forEach {
            println("${it.id} (${it.text.length}): ${it.text}")
        }
        assertThat(top20[0].id).isEqualTo(17008009)
    }

    @Test
    fun top20TokenizedVerseLength() {
        val top20Tokenized = tokenized.sortedByDescending { it.tokens.size }.take(20)
        top20Tokenized.forEach {
            println("${it.id} (${it.tokens.size} / ${it.tokens.distinct().size}): ${it.tokens}")
        }

        assertThat(top20Tokenized[0].id).isEqualTo(17008009)
        assertThat(top20Tokenized[0].tokens).hasSize(104)
    }

    @Test
    fun bitDistribution() {
        val lex = Lexicon.build(tokenized)
        val bitDistibution = tokenized.map { tv ->
            val max = tv.tokens.mapNotNull { lex.offset(it) }.maxOrNull() ?: 0
            val bits = ceil(log2(max.toFloat())).toInt()
            bits
        }.groupingBy { it }.eachCount()
        bitDistibution.toSortedMap()
            .forEach { (bits, count) -> println("Verses that can be represented using ${bits} bits : $count") }
    }

    @Test
    fun tokenStats() {
        val allTokens = tokenized.flatMap { it.tokens }.toList()
        assertThat(allTokens).hasSize(917089)
        assertThat(allTokens.distinct()).hasSize(13600)
        assertThat(allTokens.map { it.toLowerCase() }.distinct()).hasSize(12616)
        val counts = allTokens.groupingBy { it }.eachCount()
        val over1000: Map<String, Int> = counts.filterValues { it > 1000 }
        assertThat(over1000).hasSize(116)

        over1000.map { (k, v) -> v to k }.sortedByDescending { it.first }.forEach { println(it) }

        val sortedBySize = counts.toList().sortedByDescending { it.second }
        val top = sortedBySize.take(1024)
        assertThat(top).containsSequence(
            "," to 70574,
            "the" to 62064,
            "and" to 38847
        )
        top.forEachIndexed { index, pair ->
            println("$index: ${pair.second}\t${pair.first}")
        }

        val bytesNeeded = tokenized.map { verse ->
            (verse.tokens.size + 8 - 1) / 8
        }.sum()
        println("$bytesNeeded bytes are needed for bit mapping")
    }

    @Test
    fun translationComparison() {
        val translations = setOf("asv","bbe","kjv","web","ylt")
        val stats = translations.map { trans ->
            val verses = BibleCsvParser().readTranslation(trans)
            val t = VerseTokenizer()
            val tokenized = verses.map { t.tokenize(it) }
            val allTokens = tokenized.flatMap { it.tokens }.toList()
            val counts: Map<String, Int> = allTokens.groupingBy { it }.eachCount()
            trans to (counts.size to allTokens.size)
        }.toMap()

        stats.forEach { trans, tokens ->
            println("Translation $trans has ${tokens.first} distinct tokens, ${tokens.second} total")
        }
    }
}