package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat
import org.danwatt.bibcompact.huffman.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import java.util.*

class HuffmanTests {
    @Test
    fun huffmanCompressLexiconTest() {
        val verses = BibleCsvParser().readTranslation("kjv")
        val tokenizer = VerseTokenizer()
        val tokenized = verses.map { tokenizer.tokenize(it) }.toList()
        val lexicon = Lexicon.build(tokenized)

        val results: Map<String, Int> = writeHuffman(lexicon, tokenized)
        assertThat(results)
            .containsEntry("lexiconHeaderBytes", 65)//down from 123
            .containsEntry("lexiconBytes", 60924)
            .containsEntry("textHeaderBytes", 129)//Down from 13600
            .containsEntry("textBytes", 975112)
            .containsEntry("totalBytes", 65 + 60924 + 129 + 975112)
    }

    private fun writeHuffman(lexicon: Lexicon, tokenized: List<TokenizedVerse>): Map<String, Int> {
        val baos = ByteArrayOutputStream()
        val bos = BitOutputStream(baos)
        val stats = mutableMapOf<String, Int>().also {
            it.putAll(writeHuffmanLexicon(bos, lexicon))
            it.putAll(writeHuffmanText(bos, lexicon, tokenized))
        }
        bos.close()
        stats.put("totalBytes", baos.toByteArray().size)
        return stats
    }

    private fun writeHuffmanText(
        out: BitOutputStream,
        lexicon: Lexicon,
        tokenized: List<TokenizedVerse>
    ): Map<String, Int> {
        val tokenFreqs = IntArray(lexicon.getTokens().size)
        Arrays.fill(tokenFreqs, 0)
        tokenized.forEach { verse ->
            verse.tokens.forEach { token ->
                val lexiconIndex = lexicon.getLookupValue(token)
                    ?: throw IllegalArgumentException("Unknown token $token")
                tokenFreqs[lexiconIndex]++
            }
        }

        val freqs = FrequencyTable(tokenFreqs)

        val encoder = HuffmanEncoder(out, freqs.buildCodeTree())
        val codeTableSize =
            writeCanonicalCodeHeader(encoder.out, CanonicalCode(encoder.codeTree, freqs.getSymbolLimit()))

        val distribution = IntArray(21)
        Arrays.fill(distribution, 0)
        val bytesAtStart = encoder.out.bytesWritten
        tokenized.forEach { verse ->
            verse.tokens.forEach { token ->
                val position = lexicon.getLookupValue(token) ?: throw IllegalArgumentException("Unknown token $token")
                encoder.write(position)
                distribution[encoder.codeTree.getCode(position).size]++
            }
        }
        encoder.out.finishByte()
        val bytesAtEnd = encoder.out.bytesWritten

        return mapOf(
            "textBytes" to (bytesAtEnd - bytesAtStart),
            "textHeaderBytes" to codeTableSize
        )
    }

    private fun writeHuffmanLexicon(out: BitOutputStream, lexicon: Lexicon): Map<String, Int> {
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

        val freqs = FrequencyTable(initFreqs)
        val encoder = HuffmanEncoder(out, freqs.buildCodeTree())

        val codeTableSize =
            writeCanonicalCodeHeader(encoder.out, CanonicalCode(encoder.codeTree, freqs.getSymbolLimit()))

        var totalChars = 0
        val distribution = IntArray(21)
        Arrays.fill(distribution, 0)
        val bytesAtStart = encoder.out.bytesWritten
        lexicon.getTokens().forEach { token ->
            token.token.chars().forEach { char ->
                totalChars++
                distribution[encoder.codeTree.getCode(char).size]++
                encoder.write(char)
            }
            totalChars++
            encoder.write(0)
            val bits = encoder.codeTree.getCode(0)
            distribution[bits.size]++
        }
        encoder.out.finishByte()
        val bytesAtEnd = encoder.out.bytesWritten

        return mapOf(
            "lexiconBytes" to (bytesAtEnd - bytesAtStart),
            "lexiconHeaderBytes" to codeTableSize
        )
    }

    private fun writeCanonicalCodeHeader(out: BitOutputStream, canonCode: CanonicalCode): Int {
        return CanonicalCodeIO.write(canonCode, out)
    }
}