package org.danwatt.bibcompact

import org.danwatt.bibcompact.huffman.*
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import java.util.*

/* Version 2 builds on version 1, adding Huffman encoding of the lexicon and text */
class Version2Writer : BibWriter(2) {


    override fun writeHeader(verses: List<Verse>): ByteArray {
        val (books, chapterCounts, verseCounts) = countBookChapterAndVerse(verses)
        return (listOf(books.size.toByte()) + chapterCounts + verseCounts).map { it.toByte() }.toByteArray()
    }

    override fun writeVerseData(
        verses: List<TokenizedVerse>,
        lexicon: Lexicon<VerseStatsLexiconEntry>
    ): ByteArray {
        val endOfVerseMarker = lexicon.getTokens().size
        val tokenFreqs = IntArray(lexicon.getTokens().size + 1)
        Arrays.fill(tokenFreqs, 0)
        val tokens: List<Int> = verses.asSequence().flatMap { verse ->
            verse.tokens.map { token ->
                lexicon.getLookupValue(token) ?: throw IllegalArgumentException("Unknown token $token")
            }.toList() + listOf(endOfVerseMarker)
        }.toList()
        tokens.forEach { tokenFreqs[it]++ }

        val byteOutput = ByteArrayOutputStream()
        val bitOutput = BitOutputStream(byteOutput)
        val codeTree = writeHuffmanHeader(tokenFreqs, bitOutput)

        val encoder = HuffmanEncoder(bitOutput, codeTree)

        tokens.forEach { encoder.write(it) }

        encoder.out.finishByte()
        bitOutput.close()
        return byteOutput.toByteArray()
    }

    override fun writeLexicon(lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {
        val highestCharacterCode = lexicon.getTokens().asSequence()
            .flatMap { it.token.toCharArray().asSequence() }
            .maxByOrNull { it }!!
        val initFreqs = IntArray(highestCharacterCode.toInt() + 1)
        Arrays.fill(initFreqs, 0)
        lexicon.getTokens().forEach { token ->
            token.token.chars().forEach { char ->
                initFreqs[char]++
            }
            initFreqs[0]++
        }
        val byteOutput = ByteArrayOutputStream()
        val bitOutput = BitOutputStream(byteOutput)
        val actualCodeTree = writeHuffmanHeader(initFreqs, bitOutput)
        val encoder = HuffmanEncoder(bitOutput, actualCodeTree)
        var totalChars = 0

        bitOutput.writeBits(lexicon.getTokens().size, 16)

        lexicon.getTokens().forEach { token ->
            token.token.chars().forEach { char ->
                totalChars++
                encoder.write(char)
            }
            totalChars++
            encoder.write(0)
        }
        encoder.out.finishByte()
        bitOutput.close()

        return byteOutput.toByteArray()
    }

    private fun writeHuffmanHeader(
        initFreqs: IntArray,
        bitOutput: BitOutputStream
    ): CodeTree {
        val frequencies = FrequencyTable(initFreqs)
        val originalCodeTree = frequencies.buildCodeTree()
        val canonCode = CanonicalCode(originalCodeTree, frequencies.getSymbolLimit())

        CanonicalCodeIO.write(canonCode, bitOutput)

        return canonCode.toCodeTree()
    }

}