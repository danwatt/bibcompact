package org.danwatt.bibcompact.trie

import org.assertj.core.api.Assertions.assertThat
import org.danwatt.bibcompact.BibleCsvParser
import org.danwatt.bibcompact.VerseTokenizer
import org.danwatt.bibcompact.Version1WriterTest.Companion.compress
import org.danwatt.bibcompact.writeHuffmanWithTree
import org.junit.Test
import java.nio.charset.Charset
import java.util.*


internal class PrefixTrieWriterTest {
    val EOW = PrefixTrieWriter.WORD_MARKER
    val writer = PrefixTrieWriter()

    @Test
    fun simple() {
        val output = writer.write(sortedSetOf("A", "B"))
        assertThat(output).containsExactly('A', 1.c(), 'B')
    }

    @Test
    fun englishWords() {
        val wordList = sortedSetOf(
            "aardvark",
            "aardvarks",
            "aardwolf",
            "aaron",
            "aback",
            "abacus",
            "bob"
        )
        val output = PrefixTrieWriter().write(wordList)

        val expected = listOf(
            'a', 'a', 'r', 'd', 'v', 'a', 'r', 'k', EOW,
            's', 5.c(),//BS, BS, BS, BS, BS
            'w', 'o', 'l', 'f', 5.c(),//BS, BS, BS, BS, BS,
            'o', 'n', 4.c(),//BS, BS, BS, BS,
            'b', 'a', 'c', 'k', 1.c(),//BS,
            'u', 's', 6.c(),//BS, BS, BS, BS, BS, BS,
            'b', 'o', 'b'
        )
        assertThat(output).containsExactlyElementsOf(expected)
    }

    @Test
    fun peterPiper() {
        val verse =
            "Peter Piper picked a peck of pickled peppers. If Peter Piper DID pick a peck of pickled peppers, how many pickled peppers were picked by Peter Piper?";
        val words = verse.split(Regex("[^A-Za-z]"))
            .distinct()
            .filter { it.isNotEmpty() }
            .toSortedSet()
        val output = PrefixTrieWriter().write(words)
        assertThat(output).containsExactlyElementsOf(
            listOf(
                'D', 'I', 'D', 3.c(),
                'I', 'f', 2.c(),
                'P', 'e', 't', 'e', 'r', 4.c(),
                'i', 'p', 'e', 'r', 5.c(),
                'a', 1.c(),
                'b', 'y', 2.c(),
                'h', 'o', 'w', 3.c(),
                'm', 'a', 'n', 'y', 4.c(),
                'o', 'f', 2.c(),
                'p', 'e', 'c', 'k', 2.c(),
                'p', 'p', 'e', 'r', 's', 6.c(),
                'i', 'c', 'k', 31.c(),
                'e', 'd', 2.c(),
                'l', 'e', 'd', 7.c(),
                'w', 'e', 'r', 'e'
            )
        )
        assertThat(verse).hasSize(149)
        assertThat(words).hasSize(15)
        assertThat(output).hasSize(61)
    }

    @Test
    fun kjv() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val tokenizer = VerseTokenizer()
        val tokenized = verses.map { tokenizer.tokenize(it) }.toList()
        val distinctWords = tokenized.flatMap { it.tokens }.distinct()

        val output = PrefixTrieWriter().write(distinctWords.toSortedSet())
        val huff = huff(output)
        assertThat(distinctWords).hasSize(13_600)
        assertThat(distinctWords.joinToString(" ")).hasSize(109_032)
        assertThat(output).hasSize(51_703)
        assertThat(huff).hasSize(30_630)
        assertThat(compress("LZMA", output.map { it.code.toByte() }.toByteArray())).hasSize(24_593)
    }

    @Test
    fun kjvLowerCase() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val tokenizer = VerseTokenizer()
        val tokenized = verses.map { tokenizer.tokenize(it) }.toList()
        val distinctWords =
            tokenized.flatMap { it.tokens }.map { it.lowercase() }.filter { it.matches(Regex("^[a-z-]+$")) }.distinct()

        val output = PrefixTrieWriter().write(distinctWords.toSortedSet())
        val huff = huff(output)
        assertThat(distinctWords).hasSize(12_606)
        assertThat(distinctWords.joinToString(" ")).hasSize(102_300)
        assertThat(output).hasSize(47_030)
        assertThat(huff).hasSize(27_469)
        assertThat(compress("LZMA", output.map { it.code.toByte() }.toByteArray())).hasSize(22_479)
    }


    @Test
    fun `kjv with suffixes`() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val tokenizer = VerseTokenizer()
        val tokenized = verses.map { tokenizer.tokenize(it) }.toList()
        val distinctWords = tokenized.flatMap { it.tokens }.distinct()

        val top = topSuffixes(distinctWords)
        println(top)

        val output =
            PrefixTrieWriter().write(distinctWords.toSortedSet(), listOf("s", "ed", "ing", "eth", "th", "h", "d"))
        val huff = huff(output)
        assertThat(distinctWords).hasSize(13_600)
        assertThat(distinctWords.joinToString(" ")).hasSize(109_032)
        assertThat(output).hasSize(45_762)
        assertThat(huff).hasSize(28_628)
        assertThat(compress("LZMA", output.map { it.code.toByte() }.toByteArray())).hasSize(24_223)
    }

    @Test
    fun suffix() {
        val wordList = sortedSetOf(
            "act",
            "acted",
            "eat",
            "eating",
            "lunch",
            "lunches",
        )
        val output = PrefixTrieWriter().write(wordList, listOf("es", "ed", "ing"))

        val ES = (128 + 1).c()
        val ED = (128 + 2).c()
        val ING = (128 + 4).c()
        val expected = listOf(
            'e', 's', (128).c(),
            'e', 'd', (128).c(),
            'i', 'n', 'g', (128).c(),
            'a', 'c', 't', ED, 3.c(),//4 BS to eliminate the a,c,t,(ed)
            'e', 'a', 't', ING, 3.c(),
            'l', 'u', 'n', 'c', 'h', ES
        )
        println(HexFormat.ofDelimiter(" ").formatHex(expected.map { it.code.toByte() }.toList().toByteArray()))
        println(HexFormat.ofDelimiter(" ").formatHex(output.map { it.code.toByte() }.toList().toByteArray()))
        assertThat(output).containsExactlyElementsOf(expected)
    }

    @Test
    fun `suffix combinations`() {
        val wordList = sortedSetOf(
            "clock",
            "clocked",
            "clocking",
            "clocks",
            "dog",
            "dogs"
        )
        val output = PrefixTrieWriter().write(wordList, listOf("ed", "ing", "s"))


        val expected = listOf(
            'e', 'd', (128).c(),
            'i', 'n', 'g', (128).c(),
            's', (128).c(),
            'c', 'l', 'o', 'c', 'k',
            (128 + 1 + 2 + 4).c(),
            5.c(),
            'd', 'o', 'g',
            (128 + 4).c()
        )
        assertThat(output).containsExactlyElementsOf(expected)
    }

    @Test
    fun testFullWordList() {
        val lines = PrefixTrieWriter::class.java.getResourceAsStream("/en_words.txt")
            .bufferedReader(Charset.forName("UTF-8"))
            .use { it.readLines() }.distinct()
            .map { it.lowercase() }
            .toSortedSet()

        val originalSize = lines.joinToString("").length

        val output = PrefixTrieWriter().write(lines)

        assertThat(lines).hasSize(58_109)
        assertThat(originalSize).isEqualTo(484_578)
        assertThat(output).hasSize(201_749)
        val huff = huff(output)
        assertThat(huff).hasSize(115_837)
        assertThat(compress("LZMA", output.map { it.code.toByte() }.toByteArray())).hasSize(76_411)
    }

    @Test
    fun bigWordList() {
        val lines = PrefixTrieWriter::class.java.getResourceAsStream("/words_alpha.txt")
            .bufferedReader(Charset.forName("UTF-8"))
            .use { it.readLines() }.distinct()
            .map { it.lowercase() }
            .toSortedSet()

        val originalSize = lines.joinToString("").length

        val output = PrefixTrieWriter().write(lines)

        assertThat(lines).hasSize(370_103)
        assertThat(originalSize).isEqualTo(3_494_697)
        assertThat(output).hasSize(1_397_916)
        val huff = huff(output)
        assertThat(huff).hasSize(816_634)
        assertThat(compress("LZMA", output.map { it.code.toByte() }.toByteArray())).hasSize(542_572)
        assertThat(compress("LZMA", lines.joinToString("\n").toByteArray())).hasSize(886_593)
    }

    @Test
    fun singleSuffix() {
        val suffixes = PrefixTrieWriter::class.java.getResourceAsStream("/words_alpha.txt")
            .bufferedReader(Charset.forName("UTF-8"))
            .use { it.readLines() }.distinct()
            .map { it.lowercase() }
            .filter { it.length >= 3 }
            .flatMap { listOf(it.takeLast(3), it.takeLast(2), it.takeLast(1)) }
            .toList().groupingBy { it }

        val top = suffixes.eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(128)
        //println(top)

        val lines = PrefixTrieWriter::class.java.getResourceAsStream("/words_alpha.txt")
            .bufferedReader(Charset.forName("UTF-8"))
            .use { it.readLines() }.distinct()
            .map { it.lowercase() }
            .toSortedSet()

        val originalSize = lines.joinToString("").length

        val output = PrefixTrieWriter().write(lines, listOf("ed"))
        assertThat(lines).hasSize(370_103)
        assertThat(originalSize).isEqualTo(3_494_697)
        assertThat(output).hasSize(1_380_788)
        val huff = huff(output)
        assertThat(huff).hasSize(811_549)
        assertThat(compress("LZMA", output.map { it.code.toByte() }.toByteArray())).hasSize(542_276)
    }

    @Test
    fun fullWordListWithSuffixes() {
        val allWords = PrefixTrieWriter::class.java.getResourceAsStream("/en_words.txt")
            .bufferedReader(Charset.forName("UTF-8"))
            .use { it.readLines() }.distinct()
            .map { it.lowercase() }
            .toSortedSet()

        topSuffixes(allWords)

        val g = allWords.groupingBy { it.length }.eachCount().toSortedMap()
//        println(g)

        val plurals = allWords.map { allWords.contains(it + "s") }.count { it }
        assertThat(plurals).isEqualTo(11922)


        val lines = allWords.toSortedSet()

        val writer = PrefixTrieWriter()
        var out = writer.write(lines)
        assertThat(out).hasSize(201_749)
        assertThat(huff(out)).hasSize(115_837)

        out = writer.write(lines, listOf("s"))
        assertThat(out).hasSize(184_544)
        assertThat(huff(out)).hasSize(108_900)

        out = writer.write(lines, listOf("s", "d"))
        assertThat(out).hasSize(181_929)
        assertThat(huff(out)).hasSize(108_429)


        out = writer.write(lines, listOf("s", "ed"))
        assertThat(out).hasSize(178_356)
        assertThat(huff(out)).hasSize(106_644)

        out = writer.write(lines, listOf("s", "ed", "ing"))
        assertThat(out).hasSize(170_573)
        assertThat(huff(out)).hasSize(102_741)

        out = writer.write(lines, listOf("s", "ed", "ing", "d"))
        assertThat(out).hasSize(168_259)
        assertThat(huff(out)).hasSize(102_408)

        out = writer.write(lines, listOf("s", "ed", "ing", "es"))
        assertThat(out).hasSize(169_495)
        assertThat(huff(out)).hasSize(102_565)

        out = writer.write(lines, listOf("s", "ed", "ing", "es", "ly", "er", "rs"))
        assertThat(out).hasSize(164_069)
        assertThat(huff(out)).hasSize(100_908)

        out = writer.write(lines, listOf("s", "ed", "ng", "ing", "es", "ly", "er"))
        assertThat(out).hasSize(164_461)
        assertThat(huff(out)).hasSize(100_822)

        out = writer.write(lines, listOf("s", "ed", "ng", "ing", "d", "ly", "er"))
        assertThat(out).hasSize(163_322)
        assertThat(huff(out)).hasSize(100_572)
    }

    private fun topSuffixes(allWords: Collection<String>): List<Pair<String, Int>> {
        val suffixes = allWords
            .filter { it.length >= 3 }
            .flatMap { listOf(it.takeLast(4), it.takeLast(3), it.takeLast(2), it.takeLast(1)) }
            .toList().groupingBy { it }

        val top = suffixes.eachCount()
            .toList()
            .map { (first, second) -> first to (second * first.length) }
            .sortedByDescending { it.second }
            .take(128)
        return top
    }

    private fun huff(out: List<Char>) = writeHuffmanWithTree(out.map { it.code })

}

fun Int.c(): Char = Char(this)
