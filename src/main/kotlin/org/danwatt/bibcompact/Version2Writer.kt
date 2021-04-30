package org.danwatt.bibcompact

import org.danwatt.bibcompact.huffman.*
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import java.util.*

/* Version 2 builds on version 1, adding Huffman encoding of the lexicon and text */
class Version2Writer : BibWriter(2) {


    override fun writeHeader(verses: List<Verse>): ByteArray {
        val (books, chapterCounts, verseCounts) = countBookChapterAndVerse(verses)
        val asIntArray = listOf(books.size) + chapterCounts + verseCounts
        val freqs = IntArray((asIntArray.maxByOrNull { it } ?: 0)+1)
        asIntArray.forEach { freqs[it]++ }
        val baos = ByteArrayOutputStream()
        val bis = BitOutputStream(baos)
        val codeTree = writeHuffmanHeader(freqs,bis)
        val encodder = HuffmanEncoder(bis,codeTree)
        asIntArray.forEach {
            encodder.write(it)
        }
        bis.close()
        println("Encoded with huffman: ${baos.toByteArray().size}")

        return (listOf(books.size.toByte()) + chapterCounts + verseCounts).map { it.toByte() }.toByteArray()
    }

    override fun writeVerseData(
        tokenized: List<TokenizedVerse>,
        lexicon: Lexicon<VerseStatsLexiconEntry>
    ): ByteArray {
        val endOfVerseMarker = lexicon.getTokens().size
        val tokenFreqs = IntArray(lexicon.getTokens().size + 1)

        Arrays.fill(tokenFreqs, 0)
        tokenFreqs[tokenFreqs.size - 1] = tokenized.size
        tokenized.forEach { verse ->
            verse.tokens.forEach { token ->
                val lexiconIndex = lexicon.getLookupValue(token)
                    ?: throw IllegalArgumentException("Unknown token $token")
                tokenFreqs[lexiconIndex]++
            }
        }

        val baos = ByteArrayOutputStream()
        val bitOut = BitOutputStream(baos)
        val codeTree = writeHuffmanHeader(tokenFreqs, bitOut)

        val encoder = HuffmanEncoder(bitOut, codeTree)

        tokenized.forEach { verse ->
            verse.tokens.forEach { token ->
                val position = lexicon.getLookupValue(token) ?: throw IllegalArgumentException("Unknown token $token")
                encoder.write(position)
            }

            encoder.write(endOfVerseMarker)
        }
        encoder.out.finishByte()
        bitOut.close()
        return baos.toByteArray()
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
        val baos = ByteArrayOutputStream()
        val out = BitOutputStream(baos)
        val actualCodeTree = writeHuffmanHeader(initFreqs, out)
        val encoder = HuffmanEncoder(out, actualCodeTree)
        var totalChars = 0

        out.writeBits(lexicon.getTokens().size, 16)

        lexicon.getTokens().forEach { token ->
            token.token.chars().forEach { char ->
                totalChars++
                encoder.write(char)
            }
            totalChars++
            encoder.write(0)
            val bits = actualCodeTree.getCode(0)
        }
        encoder.out.finishByte()
        out.close()

        return baos.toByteArray()
    }

    private fun writeHuffmanHeader(
        initFreqs: IntArray,
        out: BitOutputStream
    ): CodeTree {
        val frequencies = FrequencyTable(initFreqs)
        val originalCodeTree = frequencies.buildCodeTree()
        val canonCode = CanonicalCode(originalCodeTree, frequencies.getSymbolLimit())

        CanonicalCodeIO.write(canonCode, out)

        return canonCode.toCodeTree()
    }

}