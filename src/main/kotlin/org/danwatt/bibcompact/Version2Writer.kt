package org.danwatt.bibcompact

import org.danwatt.bibcompact.huffman.*
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException

/* Version 2 builds on version 1, adding Huffman encoding of the lexicon and text */
class Version2Writer : BibWriter(2) {

    override fun writeVerseData(
        verses: List<TokenizedVerse>,
        lexicon: Lexicon<VerseStatsLexiconEntry>
    ): ByteArray {
        val endOfVerseMarker = lexicon.getTokens().size
        val tokens: List<Int> = verses.asSequence().flatMap { verse ->
            verse.tokens.map { token ->
                lexicon.getLookupValue(token) ?: throw IllegalArgumentException("Unknown token $token")
            }.toList() + listOf(endOfVerseMarker)
        }.toList()
        return writeHuffmanWithTree(tokens)
    }

    override fun writeLexicon(lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {
        val values = lexicon.getTokens().flatMap { token ->
            token.token.toList().map { it.toInt() }.toList() + listOf(0)
        }
        val byteOutput = ByteArrayOutputStream()
        val bitOutput = BitOutputStream(byteOutput)
        bitOutput.writeBits(lexicon.getTokens().size, 16)
        writeHuffmanWithTree(bitOutput, values)
        bitOutput.close()
        return byteOutput.toByteArray()
    }


}