package org.danwatt.bibcompact

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory
import org.danwatt.bibcompact.huffman.BitOutputStream
import org.danwatt.bibcompact.huffman.CanonicalCode
import org.danwatt.bibcompact.huffman.FrequencyTable
import java.io.ByteArrayOutputStream
import java.util.*

class Version3Writer : BibWriter(3) {
    override fun writeVerseData(
        verses: List<TokenizedVerse>,
        lexicon: Lexicon<VerseStatsLexiconEntry>
    ): ByteArray {
        val endOfVerseMarker = lexicon.getTokens().size
        val tokensSorted: Map<String, Int> =
            convertLexiconToList(lexicon).mapIndexed { index, s -> s to index }.toMap()
        val tokens: List<Int> = verses.asSequence().flatMap { verse ->
            verse.tokens.map { token -> tokensSorted[token]!! }.toList() + listOf(endOfVerseMarker)
        }.toList()
        return writeHuffmanWithTree(tokens)
    }

    override fun writeLexicon(lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {
        val bitMapping = buildBitMapping(lexicon)
        val tokens: List<String> = convertLexiconToList(lexicon)
        val tree: ConcurrentRadixTree<Int> = buildPrefixTree(tokens, bitMapping)
        val prefixTree = PrefixTreeWriter().write(tree)

        val byteOutput = ByteArrayOutputStream()
        val bitOutput = BitOutputStream(byteOutput)
        println("Prefix tree is made up of ${prefixTree.size} codes")

        bitOutput.writeBits(prefixTree.size, 24)
        writeHuffmanWithTree(bitOutput, prefixTree)
        bitOutput.close()
        return byteOutput.toByteArray()
    }

    private fun buildBitMapping(lexicon: Lexicon<VerseStatsLexiconEntry>): Map<String, Int> {
        val tokenFreqs = IntArray(lexicon.getTokens().size)
        lexicon.getTokens().forEachIndexed { inddex, token -> tokenFreqs[inddex] = token.totalOccurrences }
        val frequencies = FrequencyTable(tokenFreqs)
        val originalCodeTree = frequencies.buildCodeTree()
        val canonCode = CanonicalCode(originalCodeTree, frequencies.getSymbolLimit())
        val bitMapping = lexicon.getTokens().mapIndexed { index, token ->
            token.token to canonCode.getCodeLength(index)
        }.toMap()
        return bitMapping
    }

    private fun convertLexiconToList(lexicon: Lexicon<VerseStatsLexiconEntry>): List<String> {
        val bitMapping = buildBitMapping(lexicon)
        val c: Comparator<String> = compareBy <String> { bitMapping[it] }.thenComparing { it -> it }
        return lexicon.getTokens().map { it.token }.sortedWith(c).toList()
    }

    private fun buildPrefixTree(tokens: List<String>, bitMapping: Map<String, Int>): ConcurrentRadixTree<Int> {
        val tree: ConcurrentRadixTree<Int> = ConcurrentRadixTree(DefaultCharArrayNodeFactory())
        val distinctWords = tokens.distinct()
        val (g16, l16) = bitMapping.entries.partition { it.value >= 14 }
        println("There are ${g16.size} tokens that need 14 or more bits, ${l16.size} that need fewer")
        distinctWords.forEach {
            val bits = bitMapping[it]!!
            if (bits < 3 || bits > 25) {
                println("We might have a problem: ${bits}")
            }
            tree.put(it, bits)
        }
        return tree
    }
}

