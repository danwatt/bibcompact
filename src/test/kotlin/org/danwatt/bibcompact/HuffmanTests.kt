package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat
import org.danwatt.bibcompact.huffman.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import java.util.*

class HuffmanTests {
    private val verses = BibleCsvParser().readTranslation("kjv")
    private val tokenizer = VerseTokenizer()
    private val tokenized = verses.map { tokenizer.tokenize(it) }.toList()
    private val lexicon = Lexicon.build(tokenized)

    @Test
    fun huffmanCompressLexiconTest() {
        val (results, bytes) = writeHuffman(lexicon, tokenized)

        assertThat(results)
            .containsEntry("lexiconHeaderBytes", 64)//down from 123
            .containsEntry("lexiconBytes", 60926)
            .containsEntry("textHeaderBytes", 128)//Down from 13600
            .containsEntry("textBytes", 975112)
            .containsEntry("totalBytes", 64 + 60926 + 128 + 975112)
        /*
        If we sort the Lexicon alphabetically, we get:
        Lexicon header: 65
        Lexicon: 60924
        Text Header: 9930 (vs 129)
        Text Bytes: 975112
        So if we can save more than 10kb in the Lexicon, then its worth it to sort alphabetically
         */
        readHuffman(bytes)
    }

    @Test
    fun huffmanCompressLowerCaseOnlyLexiconTest() {
        val lower = tokenized.map {
            TokenizedVerse(it.id, it.book, it.chapter, it.verse, it.tokens.map { t -> t.toLowerCase() })
        }.toList()
        val lowerLexicon = Lexicon.build(lower)
        val (results, bytes) = writeHuffman(lowerLexicon, lower)

        assertThat(results)
            .containsEntry("lexiconHeaderBytes", 40)//down from 123
            .containsEntry("lexiconBytes", 54323)
            .containsEntry("textHeaderBytes", 123)//Down from 13600
            .containsEntry("textBytes", 954227)
            .containsEntry("totalBytes", 40 + 54323 + 123 + 954227)
        readHuffman(bytes)
    }

    private fun readHuffman(bytes: ByteArray) {
        val bais = ByteArrayInputStream(bytes)
        val bis = BitInputStream(bais)
        val lex = readHuffmanLexicon(bis)
    }

    private fun readHuffmanLexicon(bis: BitInputStream): Lexicon<TokenOnlyEntry> {
        val canonCode = CanonicalCodeIO.read(bis)
        val codeTree = canonCode.toCodeTree()

        for (i in 0..127) {
            try {
                codeTree.getCode(i)
            } catch (ex: Exception) {
                continue
            }
            println("Decoded code for $i (${i.toChar()}): ${codeTree.getCode(i).joinToString("")}")
        }

        val totalWords = bis.readBits(16)
        println("There should be $totalWords")
        val decoder = HuffmanDecoder(bis, codeTree)
        var currentWord = ""
        val words = mutableListOf<String>()
        while (words.size < totalWords) {
            val characterCode = decoder.read()
            if (characterCode == 0) {
                words.add(currentWord)
                currentWord = ""
                //End of a word
            } else {
                currentWord += characterCode.toChar()
            }
        }

        println(
            "After finishing, we are left with ${words.size} words, starting with ${
                words.subList(0, 10).joinToString(", ")
            }"
        )
        return Lexicon.buildFromWordList(words)
    }

    private fun writeHuffman(lexicon: Lexicon<VerseStatsLexiconEntry>, tokenized: List<TokenizedVerse>): Pair<Map<String, Int>, ByteArray> {
        val baos = ByteArrayOutputStream()
        val bos = BitOutputStream(baos)
        val stats = mutableMapOf<String, Int>().also {
            it.putAll(writeHuffmanLexicon(bos, lexicon))
            it.putAll(writeHuffmanText(bos, lexicon, tokenized))
        }
        bos.close()
        stats["totalBytes"] = baos.toByteArray().size
        return stats to baos.toByteArray()
    }

    private fun writeHuffmanText(
        out: BitOutputStream,
        lexicon: Lexicon<VerseStatsLexiconEntry>,
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
            CanonicalCodeIO.write(CanonicalCode(encoder.codeTree, freqs.getSymbolLimit()), encoder.out)

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

    private fun writeHuffmanLexicon(out: BitOutputStream, lexicon: Lexicon<VerseStatsLexiconEntry>): Map<String, Int> {
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
        initFreqs.forEachIndexed { index, i -> println("Index $index (${index.toChar()}): $i") }

        val freqs = FrequencyTable(initFreqs)

        val originalCodeTree = freqs.buildCodeTree()
        val canonCode = CanonicalCode(originalCodeTree, freqs.getSymbolLimit())
        val actualCodeTree = canonCode.toCodeTree()
        val codeTableSize = CanonicalCodeIO.write(canonCode, out)


        val encoder = HuffmanEncoder(out, actualCodeTree)

        var totalChars = 0
        val distribution = IntArray(21)
        Arrays.fill(distribution, 0)

        val bytesAtStart = encoder.out.bytesWritten
        out.writeBits(lexicon.getTokens().size, 16)


        for (i in 0..127) {
            try {
                actualCodeTree.getCode(i)
            } catch (ex: Exception) {
                continue
            }
            println("Code for $i (${i.toChar()}): ${actualCodeTree.getCode(i).joinToString("")}")
        }
        lexicon.getTokens().forEach { token ->
            token.token.chars().forEach { char ->
                totalChars++
                distribution[actualCodeTree.getCode(char).size]++
                encoder.write(char)
            }
            totalChars++
            encoder.write(0)
            val bits = actualCodeTree.getCode(0)
            distribution[bits.size]++
        }
        encoder.out.finishByte()
        val bytesAtEnd = encoder.out.bytesWritten

        return mapOf(
            "lexiconBytes" to (bytesAtEnd - bytesAtStart),
            "lexiconHeaderBytes" to codeTableSize
        )
    }

}
