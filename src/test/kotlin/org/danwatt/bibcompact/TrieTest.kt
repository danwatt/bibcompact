package org.danwatt.bibcompact

import com.googlecode.concurrenttrees.common.PrettyPrinter
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree
import com.googlecode.concurrenttrees.radix.node.Node
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory
import com.googlecode.concurrenttrees.radix.node.util.PrettyPrintable
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.danwatt.bibcompact.huffman.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class TrieTest {
    private val verses = BibleCsvParser().readTranslation("kjv")
    private val tokenizer = VerseTokenizer()
    private val tokenized = verses.map { tokenizer.tokenize(it) }.toList()

    /*
    we
     */

    private val END_CODE = 0
    private val PUSH_CODE = 1
    private val POP_CODE = 2

    private val LOWER_EXISTS = 2
    private val UPPER_EXISTS = 4
    private val ALL_CAPS_EXISTS = 8

    class NodeStats(
        var lowerExists: Boolean = false,
        var upperExists: Boolean = false,
        var allCapsExists: Boolean = false,
        var totalOccurrences: Int = 0,
        val actualWord: String
    )

    @Test
    fun visualizeTrie() {
        val sample =
            VerseTokenizer().tokenize("Peter Piper picked a peck of pickled peppers. If Peter Piper DID pick a peck of pickled peppers, how many pickled peppers were picked by Peter Piper?")
            //verses.filter { it.book==43 &&  it.chapter==3}.flatMap {VerseTokenizer().tokenize(it.text) }

        val lower = sample.map { it.toLowerCase() }

        val tree: ConcurrentRadixTree<NodeStats> = ConcurrentRadixTree(DefaultCharArrayNodeFactory())
        lower.forEach { token ->
            val isAlpha = Character.isAlphabetic(token.codePoints().iterator().nextInt())
            val stats = NodeStats(actualWord = token)
            if (isAlpha) {
                stats.upperExists = sample.contains(token.toLowerCase().capitalize())
                stats.lowerExists = sample.contains(token.toLowerCase())
                stats.allCapsExists = sample.contains(token.toUpperCase())
                stats.totalOccurrences = lower.count { it == token }
            }

            tree.put(token.toLowerCase(), stats)
            //PrettyPrinter.prettyPrint(tree as PrettyPrintable, System.out)
        }

        println("digraph G {")
        println("node [colorscheme=set18]")
        println(""""[root]" [shape="rectangle", label="*"]""")
        //val tNode = tree.node.outgoingEdges.first { it.incomingEdge.toString()=="t" }!!
        //printGraph(tNode, tNode.incomingEdge)
        printGraph(tree.node, "")


        println("}")
    }

    private fun printGraph(node: Node, parentNodeName: CharSequence) {
        val currentNodeName = if (parentNodeName == "") {
            "[root]"
        } else {
            val c = if (node.value == null) {
                NodeStats(actualWord = "")
            } else {
                node.value as NodeStats
            }
            var coding = 0
            if (c.lowerExists) {
                coding += 1
            }
            if (c.upperExists) {
                coding += 2
            }
            if (c.allCapsExists) {
                coding += 4
            }

            val weight = computeWeight(node)

            println(""""$parentNodeName-${node.incomingEdge}" [shape="rectangle",color="$coding",label="${node.incomingEdge}${if (weight > 1) " ($weight)" else ""} "]""")
            println(""""$parentNodeName" -> "$parentNodeName-${node.incomingEdge}" """)
            "$parentNodeName-${node.incomingEdge}"
        }
        val comparator = compareByDescending<Node> { computeWeight(it) }
            .then(compareBy { it.incomingEdge.toString() })
        node.outgoingEdges
            .sortedWith(comparator)
            .forEach { edge -> printGraph(edge, currentNodeName) }
    }

    private fun computeWeight(node: Node): Int {
        val c = if (node.value == null) {
            0
        } else {
            (node.value as NodeStats).totalOccurrences
        }
        return c + node.outgoingEdges.sumOf { computeWeight(it) }
    }

    @Test
    fun prefixTree() {
        val allTokens = tokenized.flatMap { it.tokens }.toList()
        testTrie(allTokens)
        /*
        Writing out in all lower case:
            Tree has 12616 elements
            Encoded is 61520 vs 109033
            Huffman encoded it is 32574 (code table: 51)

        Omitting the stop code when possible on same level:
            Tree has 12616 elements
            Encoded is 58198 vs 109033
            Huffman encoded it is 31328 (code table: 51)
        Omitting the PUSH code when possible:
            Tree has 12616 elements
            Encoded is 60374 vs 109033
            Huffman encoded it is 32144 (code table: 51)
            Encoded using LZMA: 24060
            Encoded using DEFLATE: 25376

        Storing lower and upper:
            Tree has 13600 elements
            Encoded is 62626 vs 109033
            Huffman encoded it is 32412 (code table: 59)
         */
    }

    @Test
    fun englishWords() {
        val stream = TrieTest::class.java.getResourceAsStream("/en_words.txt")
        val lines = stream.bufferedReader(Charset.forName("UTF-8")).use {
            it.readLines().toList()
        }
        testTrie(lines)
        //Franklin SA-98: 80,000-word dictionary stored on 128,000 bytes of read-only memory
        //Demo here: 58100 words in 123,859 bytes
    }

    private fun testTrie(allTokens: List<String>) {
        val distinct = allTokens.distinct().sorted()
        val distinctLower = distinct.map { it.toLowerCase() }.distinct().sorted().toList()

        val tree: ConcurrentRadixTree<Int> = ConcurrentRadixTree(DefaultCharArrayNodeFactory())
        //val counts = allTokens.groupingBy { it }.eachCount()
        //counts.forEach { (token, count) -> tree.put(token, count) }
        distinctLower.forEach {
            var code = 0
            if (distinct.contains(it)) {
                code += LOWER_EXISTS
            }
            if (distinct.contains(it.capitalize())) {
                code += UPPER_EXISTS
            }
            if (distinct.contains(it.toUpperCase())) {
                code += ALL_CAPS_EXISTS
            }
            tree.put(it, code)
        }
        println("Tree has " + tree.size() + " elements")
        val rawBytes = distinct.sumOf { it.length + 1 }
        val encodedTree = encodeTree(tree.node)
        println("Encoded is ${encodedTree.size} vs $rawBytes")
        val counter = IntArray(128)
        encodedTree.forEach { code ->
            counter[code]++
        }
        val freqs = FrequencyTable(counter)
        val baos = ByteArrayOutputStream()
        val bos = BitOutputStream(baos)
        val encoder = HuffmanEncoder(bos, freqs.buildCodeTree())
        val codeTableSize =
            CanonicalCodeIO.write(CanonicalCode(encoder.codeTree, freqs.getSymbolLimit()), encoder.out)
        encodedTree.forEach { code ->
            encoder.write(code)
        }
        bos.close()
        println("Huffman encoded it is ${baos.toByteArray().size} (code table: $codeTableSize)")

        val compressBaos = ByteArrayOutputStream()
        encodedTree.forEach {
            compressBaos.write(it)
        }
        compressBaos.close()
        println(
            "Encoded using LZMA: ${
                Version1WriterTest.compress(
                    CompressorStreamFactory.LZMA,
                    compressBaos.toByteArray()
                ).size
            }"
        )
        println(
            "Encoded using DEFLATE: ${
                Version1WriterTest.compress(
                    CompressorStreamFactory.DEFLATE,
                    compressBaos.toByteArray()
                ).size
            }"
        )
    }

    @Test
    fun simpleTree() {
        val tree: ConcurrentRadixTree<Int> = ConcurrentRadixTree(DefaultCharArrayNodeFactory())
        val words = listOf(
            "A",
            "!",
            "B",
            "Hello",
            "World",
            "Hell",
            "no",
            "Who",
            "What",
            "Where",
            "When",
            "Why",
            "How"
        )
        words.forEach { tree.put(it, 1) }
        val totalChars = words.sumOf { it.length + 1 }
        PrettyPrinter.prettyPrint(tree as PrettyPrintable, System.out)
        val encoded = encodeTree(tree.node)
        println(encoded)
        println("Encoded is ${encoded.size} bytes, vs $totalChars")
    }

    private fun encodeTree(node: Node): List<Int> {
        val encoded = mutableListOf<Int>()
        node.outgoingEdges.forEach { edge ->
            edge.incomingEdge.chars().forEach { c ->
                encoded.add(c)
            }
            var wroteAWordTerminatorAlready = false
            if (edge.value != null && edge.value as Int > LOWER_EXISTS) {
                encoded.add(edge.value as Int)
                wroteAWordTerminatorAlready = true
            }
            if (edge.outgoingEdges.isEmpty()) {
                if (!wroteAWordTerminatorAlready) {
                    encoded.add(END_CODE)
                }
                //TODO: Probably dont need a PUSH/POP system - maybe a DEL/BKSP?
            } else {
                encoded.add(PUSH_CODE)
                encoded.addAll(encodeTree(edge))
                encoded.add(POP_CODE)
            }
        }

        return encoded
    }
}
