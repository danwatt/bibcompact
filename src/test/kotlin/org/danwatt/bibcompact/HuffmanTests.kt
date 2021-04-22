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
    }

    private fun writeHuffman(lexicon: Lexicon, tokenized: List<TokenizedVerse>): Map<String, Int> {
        val results = mutableMapOf<String, Int>()
        results.putAll(writeHuffmanLexicon(lexicon))
        results.putAll(writeHuffmanText(lexicon, tokenized))
        return results
    }

    private fun writeHuffmanText(
        lexicon: Lexicon,
        tokenized: List<TokenizedVerse>
    ): Map<String, Int> {
        val tokenFreqs = IntArray(lexicon.getTokens().size)
        Arrays.fill(tokenFreqs, 0)
        tokenized.forEach { verse ->
            verse.tokens.forEach { token ->
                val t = lexicon.getFullTokenStats(token)
                val lexiconIndex = lexicon.getLookupValue(token)
                    ?: throw IllegalArgumentException("Unknown token $token")
                tokenFreqs[lexiconIndex]++
            }
        }

        val freqs = FrequencyTable(tokenFreqs)
        val baos = ByteArrayOutputStream()

        val encoder = HuffmanEncoder(BitOutputStream(baos), freqs.buildCodeTree())
        val codeTableSize =
            writeCanonicalCodeHeader(encoder.out, CanonicalCode(encoder.codeTree, freqs.getSymbolLimit()))

        val distribution = IntArray(21)
        Arrays.fill(distribution, 0)
        tokenized.forEach { verse ->
            verse.tokens.forEach { token ->
                val position = lexicon.getLookupValue(token) ?: throw IllegalArgumentException("Unknown token $token")
                encoder.write(position)
                distribution[encoder.codeTree.getCode(position).size]++
            }
        }
        encoder.out.close()
        println("Huffman encoded text length : " + baos.size())
        println("Bit distribution:")
        distribution.forEachIndexed { index, i ->
            if (i > 0) {
                println("$index bits: $i")
            }
        }
        return mapOf(
            "textBytes" to baos.size() - codeTableSize,
            "textHeaderBytes" to codeTableSize
        )
    }

    private fun writeHuffmanLexicon(lexicon: Lexicon): Map<String, Int> {
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
        val baos = ByteArrayOutputStream()
        val encoder = HuffmanEncoder(BitOutputStream(baos), freqs.buildCodeTree())

        val codeTableSize =
            writeCanonicalCodeHeader(encoder.out, CanonicalCode(encoder.codeTree, freqs.getSymbolLimit()))

        var totalChars = 0
        val distribution = IntArray(21)
        Arrays.fill(distribution, 0)
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
        encoder.out.close()

        println("Baos has length: ${baos.size()}, compared to $totalChars raw")
        println("Bit distribution:")
        distribution.forEachIndexed { index, i ->
            if (i > 0) {
                println("$index bits: $i")
            }
        }
        return mapOf(
            "lexiconBytes" to baos.size() - codeTableSize,
            "lexiconHeaderBytes" to codeTableSize
        )
    }

    private fun writeCanonicalCodeHeader(out: BitOutputStream, canonCode: CanonicalCode): Int {
        return CanonicalCodeIO.write(canonCode,out)
        /*
        var longestCode = 0
        var shortest = Int.MAX_VALUE
        val distribution = IntArray(21)
        Arrays.fill(distribution, 0)
        var bytesWritten = 0
        for (i in 0 until canonCode.getSymbolLimit()) {
            val v = canonCode.getCodeLength(i)
            longestCode = kotlin.math.max(longestCode, v)
            if (v > 0) {
                shortest = kotlin.math.min(shortest, v)
            }
            distribution[v]++
            if (v >= 256) throw RuntimeException("The code for a symbol is too long")
            // Write value as 8 bits in big endian
            for (j in 7 downTo 0) out.write(v ushr j and 1)
            bytesWritten++
            print("${v.toString(16).padStart(2,'0')} ")
            if ((i+1) % 32 == 0) {
                println()
            }
        }

        println("Longest code encountered: $longestCode bits, shortest: $shortest")
        distribution.forEachIndexed { index, i ->
            println("$index: $i")
        }
        return bytesWritten*/
    }
}