package org.danwatt.bibcompact

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PrefixTreeTests {
    private val endOfWord = listOf(0)
    private val push = listOf(1)
    private val pop = listOf(2)

    private val writer = PrefixTreeWriter()
    private val reader = PrefixTreeReader()

    @Test
    fun singleItemTree() {
        val tree: ConcurrentRadixTree<Boolean> = buildTree(listOf("tree"))
        val codes = writer.write(tree)

        assertThat(codes).isEqualTo("tree".toCodes() + endOfWord)
        readAndAssertEqual(codes, tree)
    }

    private fun readAndAssertEqual(
        codes: List<Int>,
        tree: ConcurrentRadixTree<Boolean>
    ) {
        val readTree = reader.read(codes)
        //println("Results:")
        //PrettyPrinter.prettyPrint(readTree as PrettyPrintable, System.out)
        assertThat(readTree.getKeysStartingWith("").toList()).isEqualTo(
            tree.getKeysStartingWith("").toList()
        )
    }

    @Test
    fun twoUnrelatedWords() {
        val tree: ConcurrentRadixTree<Boolean> = buildTree(listOf("hello", "world"))
        val codes = writer.write(tree)
        assertThat(codes).isEqualTo("hello".toCodes() + endOfWord + "world".toCodes() + endOfWord)
        readAndAssertEqual(codes, tree)
    }

    @Test
    fun twoWordsThatBuildOnEachOther() {
        val tree: ConcurrentRadixTree<Boolean> = buildTree(listOf("tree", "trees"))

        val codes = writer.write(tree)
        assertThat(codes).isEqualTo("tree".toCodes() + push + "s".toCodes() + endOfWord + pop + endOfWord)
        readAndAssertEqual(codes, tree)
    }


    @Test
    fun twoWordsWithSharedPrefixButPrefixIsNotAAWord() {
        val tree: ConcurrentRadixTree<Boolean> = buildTree(listOf("tree", "treat"))

        val codes = writer.write(tree)
        assertThat(codes).isEqualTo(
            "tre".toCodes()
                    + push +
                    "at".toCodes() + endOfWord +
                    "e".toCodes() + endOfWord
                    + pop
        )
        readAndAssertEqual(codes, tree)
    }

    @Test
    fun blogSample1() {
        val list = listOf("Peter", "Piper", "pickled", "picked", "peck", "peppers")
        val tree: ConcurrentRadixTree<Boolean> = buildTree(list)
        val codes = writer.write(tree)

        //@formatter:off
        assertThat(codes).isEqualTo(
//Depth 0
            "P".toCodes()
                    + push
                        + "eter".toCodes() + endOfWord
                        + "iper".toCodes() + endOfWord
                    + pop
// Depth 0
            + "p".toCodes()
                + push
                    + "e".toCodes()
                        + push
                            + "ck".toCodes() + endOfWord
                            + "ppers".toCodes() + endOfWord
                        + pop
                    + "ick".toCodes()
                        + push
                            + "ed".toCodes() + endOfWord
                            + "led".toCodes() + endOfWord
                        + pop
                + pop
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
        val tree: ConcurrentRadixTree<Boolean> = buildTree(list)
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
                    + "iper".toCodes() + endOfWord
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
                            + "ppers".toCodes() + endOfWord
                        + pop
                    + "ick".toCodes()
                        + push
                            + "ed".toCodes() + endOfWord
                            + "led".toCodes() + endOfWord
                        + pop
                    + endOfWord //picks
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
        val tree: ConcurrentRadixTree<Boolean> = buildTree(distinctWords)
        val codes = writer.write(tree)

        println("KJV lexicon can be encoded as ${codes.size} codes")

        val huffmanEncoded = writeHuffmanWithTree(codes)
        println("Encoded using huffman ${huffmanEncoded.size}")

        readAndAssertEqual(codes, tree)

        /*
        Lower case:
        KJV lexicon can be encoded as 60270 codes
        Encoded using huffman 30273

        Case sensitive
        KJV lexicon can be encoded as 65990 codes
        Encoded using huffman 33609
         */
    }
}

private fun String.toCodes(): List<Int> = this.asSequence().map { it.toInt() }.toList()

private fun buildTree(words: List<String>): ConcurrentRadixTree<Boolean> {
    val tree: ConcurrentRadixTree<Boolean> = ConcurrentRadixTree(DefaultCharArrayNodeFactory())
    words.forEach {
        tree.put(it, true)
    }
    return tree
}