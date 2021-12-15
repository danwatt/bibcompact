package org.danwatt.bibcompact

import com.googlecode.concurrenttrees.common.PrettyPrinter
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory
import com.googlecode.concurrenttrees.radix.node.util.PrettyPrintable
import org.assertj.core.api.Assertions.assertThat
import org.danwatt.bibcompact.radixtree.PrefixTreeWriter.Companion.POP_CODE
import org.danwatt.bibcompact.radixtree.PrefixTreeWriter.Companion.PUSH_CODE
import org.danwatt.bibcompact.radixtree.PrefixTreeReader
import org.danwatt.bibcompact.radixtree.PrefixTreeWriter
import org.junit.Test
import java.nio.charset.Charset

class PrefixTreeTests {
    private val push = listOf(PUSH_CODE)
    private val pop = listOf(POP_CODE)

    private val writer = PrefixTreeWriter()
    private val reader = PrefixTreeReader()

    @Test
    fun singleItemTree() {
        val tree: ConcurrentRadixTree<Int> = buildTree(listOf("tree"))
        val codes = writer.write(tree)

        assertThat(codes).isEqualTo("tree".toCodes() + listOf(4))
        val t = readAndAssertEqual(codes, tree)
        PrettyPrinter.prettyPrint(t as PrettyPrintable, System.out)
        assertThat(t.getValueForExactKey("tree")).isEqualTo(4)
    }

    private fun readAndAssertEqual(
        codes: List<Int>,
        tree: ConcurrentRadixTree<Int>
    ): ConcurrentRadixTree<Int> {
        //println("Reading codes : ${codes.joinToString(", ")}")
        val readTree = reader.read(codes)
        //println("Results:")
        //PrettyPrinter.prettyPrint(readTree as PrettyPrintable, System.out)
        assertThat(readTree.getKeysStartingWith("").toList()).isEqualTo(
            tree.getKeysStartingWith("").toList()
        )
        return readTree
    }

    @Test
    fun twoUnrelatedWords() {
        val tree: ConcurrentRadixTree<Int> = buildTree(listOf("hello", "world"))
        val codes = writer.write(tree)
        assertThat(codes).isEqualTo("hello".toCodes() + listOf(5) + "world".toCodes() + listOf(5))
        readAndAssertEqual(codes, tree)
    }

    @Test
    fun twoWordsThatBuildOnEachOther() {
        val tree: ConcurrentRadixTree<Int> = buildTree(listOf("tree", "trees"))

        val codes = writer.write(tree)
        //@formatter:off
        assertThat(codes).isEqualTo(
            "tree".toCodes() + listOf(4)
                + push
                    + "s".toCodes() + listOf(5)
        )
        //@formatter:on
        readAndAssertEqual(codes, tree)
    }


    @Test
    fun twoWordsWithSharedPrefixButPrefixIsNotAAWord() {
        val tree: ConcurrentRadixTree<Int> = buildTree(listOf("tree", "treat"))

        val codes = writer.write(tree)
        //@formatter:off
        assertThat(codes).isEqualTo(
            "tre".toCodes()
                    + push
                        + "at".toCodes() + listOf(5)
                        + "e".toCodes()  + listOf(4)
        )
        //@formatter:on
        readAndAssertEqual(codes, tree)
    }

    @Test
    fun blogSample1() {
        val list = listOf("Peter", "Piper", "pickled", "picked", "peck", "peppers")
        val tree: ConcurrentRadixTree<Int> = buildTree(list)
        val codes = writer.write(tree)

        //@formatter:off
        assertThat(codes).isEqualTo(
            "P".toCodes()
                    + push
                        + "eter".toCodes() + listOf(5)
                        + "iper".toCodes() + listOf(5)
                    + pop
            + "p".toCodes()
                + push
                    + "e".toCodes()
                        + push
                            + "ck".toCodes() + listOf(4)
                            + "ppers".toCodes() + listOf(7)
                        + pop
                    + "ick".toCodes()
                        + push
                            + "ed".toCodes() + listOf(6)
                            + "led".toCodes() + listOf(7)
        )
        //@formatter:on
        readAndAssertEqual(codes, tree)
    }

    @Test
    fun fullPeterPiper() {
        val words = """
            Peter Piper picked a peck of pickled peppers ;
            A peck of pickled peppers Peter Piper picked ;
            If Peter Piper picked a peck of pickled peppers ,
            Where's the peck of pickled peppers Peter Piper picked ?""".trimIndent()
            .split(" ")
            .filter { it.isNotEmpty() }
            .distinct()
        val tree: ConcurrentRadixTree<Int> = buildTree(words)
        val codes = writer.write(tree)
        codes.forEach {
            when {
                it == 30 || it == 31 -> println(it)
                it > 31 -> print(it.toChar() + " ")
                else -> print(it.toString() + " ")
            }
        }
    }

    @Test
    fun blogSample2() {
        val list = listOf(
            "Peter",
            "Piper",
            "picked",
            "a",
            "peck",
            "of",
            "pickled",
            "peppers",
            ".",
            "If",
            "Peter",
            "Piper",
            "DID",
            "pick",
            "a",
            "peck",
            "of",
            "pickled",
            "peppers",
            ",",
            "how",
            "many",
            "pickled",
            "peppers",
            "were",
            "picked",
            "by",
            "Peter",
            "Piper",
            "?"
        ).distinct()
        val tree: ConcurrentRadixTree<Int> = buildTree(list)
        val codes = writer.write(tree)
        readAndAssertEqual(codes, tree)
        //@formatter:off
        assertThat(codes).isEqualTo(
            ",".toCodes() + listOf(1)
            +".".toCodes() + listOf(1)
            +"?".toCodes() + listOf(1)
            +"DID".toCodes() + listOf(3)
            +"If".toCodes() + listOf(2)
            + "P".toCodes()
                + push
                    + "eter".toCodes() + listOf(5)
                    + "iper".toCodes() + listOf(5)
                + pop
            +"a".toCodes() + listOf(1)
            +"by".toCodes() + listOf(2)
            +"how".toCodes() + listOf(3)
            +"many".toCodes() + listOf(4)
            +"of".toCodes() + listOf(2)
            +"p".toCodes()
                + push
                    + "e".toCodes()
                        + push
                            + "ck".toCodes() + listOf(4)
                            + "ppers".toCodes() + listOf(7)
                        + pop
                    + "ick".toCodes() + listOf(4)
                        + push
                            + "ed".toCodes() + listOf(6)
                            + "led".toCodes() + listOf(7)
                        + pop
                + pop
            + "were".toCodes() + listOf(4)
        )
    }

    @Test
    fun kjvTest() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val tokenizer = VerseTokenizer()
        val tokenized = verses.map { tokenizer.tokenize(it) }.toList()
        val distinctWords = tokenized.flatMap { it.tokens }.distinct()
        val tree: ConcurrentRadixTree<Int> = buildTree(distinctWords)
        val codes = writer.write(tree)
        val huffmanEncoded = writeHuffmanWithTree(codes)

        readAndAssertEqual(codes, tree)

        assertThat(codes).hasSize(65988)
        assertThat(huffmanEncoded).hasSize(39011)

        /*
        Lower case:
        KJV lexicon can be encoded as 60270 codes
        Encoded using huffman 30273

        Case sensitive:
        KJV lexicon can be encoded as 65990 codes
        Encoded using huffman 33609

        Storing with a `0` after every complete word
        65988 codes / 33611 huffman


        Case markers:
        Tree has 12616 entries
        KJV lexicon can be encoded as 60270 codes
        Encoded using huffman 32177

        Removing unnecessaary POP / EOW:
        KJV lexicon can be encoded as 62247 codes
        Encoded using huffman 32366

        Removing unnecessary POP and case markers
        Tree has 12616 entries
        Before cleanup : 60270
        After cleanup : 56196
        KJV lexicon can be encoded as 56196 codes
        Encoded using huffman 30481
         */
    }

    @Test
    fun englishWordListTest() {
        val lines = PrefixTreeTests::class.java.getResourceAsStream("/en_words.txt")
            .bufferedReader(Charset.forName("UTF-8")).use {
            it.readLines()
        }.distinct().sorted()
        val tree: ConcurrentRadixTree<Int> = buildTree(lines)
        val codes = writer.write(tree)
        val huffmanEncoded = writeHuffmanWithTree(codes)

        readAndAssertEqual(codes, tree)

        assertThat(codes).hasSize(265114)
        assertThat(huffmanEncoded).hasSize(153216)//If we omitted the value, it would be a whole lot less


    }
}

private fun String.toCodes(): List<Int> = this.asSequence().map { it.toInt() }.toList()

private fun buildTree(words: List<String>): ConcurrentRadixTree<Int> {
    val distinctWords = words.distinct()
    val tree: ConcurrentRadixTree<Int> = ConcurrentRadixTree(DefaultCharArrayNodeFactory())
    distinctWords.forEach {
        // For test purposes, we are just going to write the length as the value
        // In the actual implementation elsewhere in this project, the value will be a number relative to the number
        // of bits needed to huffman encode the word
        tree.put(it, it.length)
    }
    return tree
}