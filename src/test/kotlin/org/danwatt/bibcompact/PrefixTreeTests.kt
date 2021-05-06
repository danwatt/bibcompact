package org.danwatt.bibcompact

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.charset.Charset

class PrefixTreeTests {
    private val endOfWord = listOf(0)
    private val push = listOf(1)
    private val pop = listOf(2)

    private val writer = PrefixTreeWriter()
    private val reader = PrefixTreeReader()

    @Test
    fun singleItemTree() {
        val tree: ConcurrentRadixTree<Int> = buildTree(listOf("tree"))
        val codes = writer.write(tree)

        assertThat(codes).isEqualTo("tree".toCodes() + endOfWord)
        readAndAssertEqual(codes, tree)
    }

    private fun readAndAssertEqual(
        codes: List<Int>,
        tree: ConcurrentRadixTree<Int>
    ) {
        //println("Reading codes : ${codes.joinToString(", ")}")
        val readTree = reader.read(codes)
        //println("Results:")
        //PrettyPrinter.prettyPrint(readTree as PrettyPrintable, System.out)
        assertThat(readTree.getKeysStartingWith("").toList()).isEqualTo(
            tree.getKeysStartingWith("").toList()
        )
    }

    @Test
    fun twoUnrelatedWords() {
        val tree: ConcurrentRadixTree<Int> = buildTree(listOf("hello", "world"))
        val codes = writer.write(tree)
        assertThat(codes).isEqualTo("hello".toCodes() + endOfWord + "world".toCodes() + endOfWord)
        readAndAssertEqual(codes, tree)
    }

    @Test
    fun twoWordsThatBuildOnEachOther() {
        val tree: ConcurrentRadixTree<Int> = buildTree(listOf("tree", "trees"))

        val codes = writer.write(tree)
        //@formatter:off
        assertThat(codes).isEqualTo(
            "tree".toCodes() + endOfWord
                + push
                    + "s".toCodes()
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
                        + "at".toCodes() + endOfWord
                        + "e".toCodes() //+ endOfWord
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
//Depth 0
            "P".toCodes()
                    + push
                        + "eter".toCodes() + endOfWord
                        + "iper".toCodes()
                    + pop
// Depth 0
            + "p".toCodes()
                + push
                    + "e".toCodes()
                        + push
                            + "ck".toCodes() + endOfWord
                            + "ppers".toCodes()
                        + pop
                    + "ick".toCodes()
                        + push
                            + "ed".toCodes() + endOfWord
                            + "led".toCodes()
        )
        //@formatter:on
        readAndAssertEqual(codes, tree)
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
            ",".toCodes() + endOfWord
            +".".toCodes() + endOfWord
            +"?".toCodes() + endOfWord
            +"DID".toCodes() + endOfWord
            +"If".toCodes() + endOfWord
            + "P".toCodes()
                + push
                    + "eter".toCodes() + endOfWord
                    + "iper".toCodes()
                + pop
            +"a".toCodes() + endOfWord
            +"by".toCodes() + endOfWord
            +"how".toCodes() + endOfWord
            +"many".toCodes() + endOfWord
            +"of".toCodes() + endOfWord
            +"p".toCodes()
                + push
                    + "e".toCodes()
                        + push
                            + "ck".toCodes() + endOfWord
                            + "ppers".toCodes()
                        + pop
                    + "ick".toCodes() + endOfWord
                        + push
                            + "ed".toCodes() + endOfWord
                            + "led".toCodes()
                        + pop
                + pop
            + "were".toCodes() + endOfWord
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

        assertThat(codes).hasSize(60736)
        assertThat(huffmanEncoded).hasSize(31799)

        /*
        Lower case:
        KJV lexicon can be encoded as 60270 codes
        Encoded using huffman 30273

        Case sensitive:
        KJV lexicon can be encoded as 65990 codes
        Encoded using huffman 33609


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
        }
        val tree: ConcurrentRadixTree<Int> = buildTree(lines)
        val codes = writer.write(tree)
        val huffmanEncoded = writeHuffmanWithTree(codes)

        readAndAssertEqual(codes, tree)

        assertThat(codes).hasSize(242391)
        assertThat(huffmanEncoded).hasSize(121766)
    }
}

private fun String.toCodes(): List<Int> = this.asSequence().map { it.toInt() }.toList()

private fun buildTree(words: List<String>): ConcurrentRadixTree<Int> {
    val tree: ConcurrentRadixTree<Int> = ConcurrentRadixTree(DefaultCharArrayNodeFactory())
    val distinctWords = words.distinct()
    distinctWords.forEach {
        /*val lowerExists = distinctWords.contains(it.toLowerCase())
        val capitalizedExists = distinctWords.contains(it.capitalize())
        val upperCaseExists = distinctWords.contains(it.toUpperCase())
        var flag = 0
        if (lowerExists) {
            flag += 1
        }
        if (capitalizedExists) {
            flag += 2
        }
        if (upperCaseExists) {
            flag += 4
        }
        tree.put(it, flag)
         */
        tree.put(it, 0)
    }
    return tree
}