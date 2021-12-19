package org.danwatt.bibcompact

import org.danwatt.bibcompact.huffman.BitOutputStream
import org.danwatt.bibcompact.trie.PrefixTrieWriter
import java.io.ByteArrayOutputStream

class Version4Writer : Version3Writer(4) {

    override fun writeLexicon(lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {
        val sortedLexicon = lexicon.getTokens().map { it.token }.toSortedSet()

        val trieChars: List<Char> = PrefixTrieWriter().write(
            sortedLexicon,
            listOf("s", "ed", "ing", "eth", "th", "h", "d")
        )

        val byteOutput = ByteArrayOutputStream()
        val lexiconBitOutput = BitOutputStream(byteOutput)
        lexiconBitOutput.writeBits(lexicon.getTokens().size, 16)
        writeHuffmanWithTree(lexiconBitOutput, trieChars.map { it.code })
        lexiconBitOutput.close()

        val bitMapping = buildBitMapping(lexicon)
        val bitAllotments = sortedLexicon.mapNotNull { bitMapping[it] }.toList()
        val wordBitMappingOutput = BitOutputStream(byteOutput)
        writeHuffmanWithTree(wordBitMappingOutput, bitAllotments)
        wordBitMappingOutput.close()
        return byteOutput.toByteArray()
    }
}

