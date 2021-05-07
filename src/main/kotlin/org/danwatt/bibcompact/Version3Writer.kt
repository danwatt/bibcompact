package org.danwatt.bibcompact

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory
import org.danwatt.bibcompact.huffman.BitOutputStream
import org.danwatt.bibcompact.huffman.HuffmanEncoder
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import java.util.Comparator

class Version3Writer : BibWriter(3) {
    override fun writeVerseData(
        verses: List<TokenizedVerse>,
        lexicon: Lexicon<VerseStatsLexiconEntry>
    ): ByteArray {
        val endOfVerseMarker = lexicon.getTokens().size
        val tokensInAlphabetical: Map<String, Int> =
            convertLexiconToList(lexicon).mapIndexed { index, s -> s to index }.toMap()
        val tokens: List<Int> = verses.asSequence().flatMap { verse ->
            verse.tokens.map { token -> tokensInAlphabetical[token]!! }.toList() + listOf(endOfVerseMarker)
        }.toList()
        return writeHuffmanWithTree(tokens)
    }


    override fun writeLexicon(lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {
        val byteOutput = ByteArrayOutputStream()
        val bitOutput = BitOutputStream(byteOutput)
        bitOutput.writeBits(lexicon.getTokens().size, 16)
        val tokens = convertLexiconToList(lexicon)
        val tree: ConcurrentRadixTree<Int> = buildTree(tokens)
        val prefixTree = PrefixTreeWriter().write(tree)
        writeHuffmanWithTree(bitOutput, prefixTree)
        bitOutput.close()
        return byteOutput.toByteArray()
    }

    private fun convertLexiconToList(lexicon: Lexicon<VerseStatsLexiconEntry>): List<String> {
        val c: Comparator<String> = compareBy<String> { it.length }.thenComparing { it -> it }
        return lexicon.getTokens().map { it.token }.sortedWith(c).toList()
    }

    private fun buildTree(tokens: List<String>): ConcurrentRadixTree<Int> {
        val tree: ConcurrentRadixTree<Int> = ConcurrentRadixTree(DefaultCharArrayNodeFactory())
        val distinctWords = tokens.distinct()
        distinctWords.forEach {
            tree.put(it, 0)
        }
        return tree
    }
}

