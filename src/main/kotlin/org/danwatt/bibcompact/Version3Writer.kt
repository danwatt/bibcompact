package org.danwatt.bibcompact

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory
import org.danwatt.bibcompact.huffman.BitOutputStream
import org.danwatt.bibcompact.huffman.CanonicalCode
import org.danwatt.bibcompact.huffman.FrequencyTable
import org.danwatt.bibcompact.radixtree.PrefixTreeWriter
import java.io.ByteArrayOutputStream
import java.util.*

open class Version3Writer(version: Int = 3) : BibWriter(version) {
    override fun writeVerseData(
        verses: List<TokenizedVerse>,
        lexicon: Lexicon<VerseStatsLexiconEntry>
    ): ByteArray {
        val endOfVerseMarker = 0
        val tokensToFrequency: Map<String, Int> =
            convertLexiconToList(lexicon).mapIndexed { index, s -> s to index + 1 }.toMap()
        val tokens: List<Int> = verses.asSequence().flatMap { verse ->
            verse.tokens.map { token -> tokensToFrequency[token]!! }.toList() + listOf(endOfVerseMarker)
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
        bitOutput.writeBits(prefixTree.size, 24)
        writeHuffmanWithTree(bitOutput, prefixTree)
        bitOutput.close()
        return byteOutput.toByteArray()
    }

    fun buildBitMapping(lexicon: Lexicon<VerseStatsLexiconEntry>): Map<String, Int> {
        val tokens = lexicon.getTokens()
        val tokenFreqs = IntArray(tokens.size + 1)
        tokenFreqs[0] = Integer.MAX_VALUE
        tokens.forEachIndexed { index, token -> tokenFreqs[index + 1] = token.totalOccurrences }
        val frequencies = FrequencyTable(tokenFreqs)
        val originalCodeTree = frequencies.buildCodeTree()
        val canonCode = CanonicalCode(originalCodeTree, frequencies.getSymbolLimit())
        val bitMapping = tokens.mapIndexed { index, token ->
            token.token to canonCode.getCodeLength(index + 1)
        }.toMap()
        return bitMapping
    }

    private fun convertLexiconToList(lexicon: Lexicon<VerseStatsLexiconEntry>): List<String> {
        val bitMapping = buildBitMapping(lexicon)
        val c: Comparator<String> = compareBy<String> { bitMapping[it] }.thenComparing { it -> it }
        return lexicon.getTokens().map { it.token }.sortedWith(c).toList()
    }

    private fun buildPrefixTree(tokens: List<String>, bitMapping: Map<String, Int>): ConcurrentRadixTree<Int> {
        val tree: ConcurrentRadixTree<Int> = ConcurrentRadixTree(DefaultCharArrayNodeFactory())
        tokens.distinct().forEach {
            val bits = bitMapping[it]!!
            tree.put(it, bits)
        }
        return tree
    }
}

