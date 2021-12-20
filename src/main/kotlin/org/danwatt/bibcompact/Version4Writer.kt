package org.danwatt.bibcompact

import org.danwatt.bibcompact.huffman.BitOutputStream
import org.danwatt.bibcompact.trie.PrefixTrieWriter
import java.io.ByteArrayOutputStream

class Version4Writer : Version3Writer(4) {

    /* Lexicon format:
    4 bytes (B): The number of huffman codes that comprise the Trie
    B codepoints : the trie structure itself
    4 bytes (C): The number of items in the lexicon
    C codepoints : the bit allocations of each word (canonicalized huffman)
     */

    override fun writeLexicon(lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {
        val sortedLexicon = lexicon.getTokens().map { it.token }.toSortedSet()
        val byteOutput = ByteArrayOutputStream()

        writePrefixTrie(byteOutput, sortedLexicon)
        val trieSize = byteOutput.size()
        writeBitAllocations(byteOutput, lexicon, sortedLexicon)
        val allocationSize = byteOutput.size() - trieSize

        println("Trie: ${trieSize}. Allocation: ${allocationSize}. Total: ${byteOutput.size()}")

        return byteOutput.toByteArray()
    }

    private fun writeBitAllocations(
        byteOutput: ByteArrayOutputStream,
        lexicon: Lexicon<VerseStatsLexiconEntry>,
        sortedLexicon: Collection<String>
    ) {
        val bitMapping = buildBitMapping(listOf(Integer.MAX_VALUE), lexicon.getTokens())
        val bitAllotments = sortedLexicon.mapNotNull { bitMapping[it] }.toList()
        val wordBitMappingOutput = BitOutputStream(byteOutput)
        wordBitMappingOutput.writeBits(bitAllotments.size, 32)
        writeHuffmanWithTree(wordBitMappingOutput, bitAllotments)
        wordBitMappingOutput.close()
    }

    private fun writePrefixTrie(
        byteOutput: ByteArrayOutputStream,
        sortedLexicon: Collection<String>
    ) {
        val trieChars: List<Char> = PrefixTrieWriter().write(
            sortedLexicon,
            //TODO: Make this dynamic
            listOf("s", "ed", "ing", "eth", "th", "h", "d")
        )

        val lexiconBitOutput = BitOutputStream(byteOutput)
        lexiconBitOutput.writeBits(trieChars.size, 32)
        writeHuffmanWithTree(lexiconBitOutput, trieChars.map { it.code })
        lexiconBitOutput.close()
    }
}

