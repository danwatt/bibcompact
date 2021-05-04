package org.danwatt.bibcompact

import org.danwatt.bibcompact.huffman.*
import java.io.ByteArrayOutputStream
import java.util.*

class Version4Writer(val stopWords: Set<String>) : BibWriter(4) {
    /*
    What to do with the end-of verse marker? There will be 31k of them, so it will be one of the most common tokens
    There are some verses that are longer than 256 tokens, so we would need 9-10 bits to store these (~38kb)
    Encoded using Huffman it is probably going to be 3-4 bits
    We likely need to store it in the "search" file
     */

    override fun writeVerseData(verses: List<TokenizedVerse>, lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {
        bookCompressionTest(verses,lexicon)
        //We will be writing three bitstreams:
        //1) A 0/1 to indicate if the word is a stop word or a search word
        //2) The search word file
        //3) The stop word file

        val (stopWordList, searchWordList) = lexicon.getTokens().map { it.token }
            .partition { stopWords.contains(it.toLowerCase()) }

        val toggle = mutableListOf<Int>()
        val searchFile = mutableListOf<Int>()
        val stopWordFile = mutableListOf<Int>()

        val endOfVerseMarker = 0

        verses.asSequence().forEach { verse ->
            verse.tokens.forEach {
                if (stopWordList.contains(it)) {
                    toggle.add(0)
                    stopWordFile.add(stopWordList.indexOf(it) + 1)
                } else {
                    toggle.add(1)
                    stopWordFile.add(0)
                    searchFile.add(searchWordList.indexOf(it) + 1)
                }
            }
            toggle.add(1)
            searchFile.add(endOfVerseMarker)
        }

        val switchFileBytes = encodeBits(toggle)
        val searchFileBytes = encodeIntegers(searchFile)
        val stopWordFileBytes = encodeIntegers(stopWordFile)
        println("Switch file :${switchFileBytes.size}")//118524, or 118528 as Huffman codes
        println("Search file :${searchFileBytes.size}")
        println("Stopword file :${stopWordFileBytes.size}")

        stopwordTest(stopWordFile.filter { it > 0 })

        for (skip in 6..64) {
            //testSkips(skip, searchFile)
        }

        /* Using three files :
        Switch file :118528
        Search file :478561
        Stopword file :405160
        Total: 1,002,249
         */

        /* Using two files, with placeholders in the stopword file:
        Search file :458591
        Stopword file :545711
        Total: 1,004,302
         */

        /* Using two files, with placeholders in the stopword file, verse marker in searchFile
        Search file :466369
        Stopword file :519801
        Total: 986,170
         */

        //20 skips: 467838
        //32 skips: 468708

        /*
        Using the switch file and ngrams:
        Switch: 118524
        Search: 478561
        Stop: 16: 396329    8: 396756 4: 395235 2: 400083
        Total: 993,414
         */

        return /*switchFileBytes +*/ searchFileBytes + stopWordFileBytes
    }

    private fun bookCompressionTest(verses: List<TokenizedVerse>, lexicon: Lexicon<VerseStatsLexiconEntry>) {
        val tokens: List<VerseStatsLexiconEntry> =     lexicon.getTokens()
        val byBook: Map<Int, List<TokenizedVerse>> = verses.groupBy { it.book }

        var totalBytes = 0

        byBook.forEach { (bookNumber, bookVerses) ->
            val compressedData1 = bookCompresssion1(bookVerses, tokens, bookNumber)
            val compressedData2 = bookCompresssion2(bookVerses, tokens, bookNumber)
            val compressedData3 = bookCompresssion3(bookVerses, tokens, bookNumber)
            totalBytes+= compressedData1.size

            println("$bookNumber\t${compressedData1.size}\t${compressedData2.size}\t${compressedData3.size}")
        }
        println("Encoding each book separately is $totalBytes")
    }
    // Option 2 : Just use the offsets in the Lexicon
    private fun bookCompresssion2(
        bookVerses: List<TokenizedVerse>,
        tokens: List<VerseStatsLexiconEntry>,
        bookNumber: Int
    ): ByteArray {
        val fileData: List<Int> = bookVerses.asSequence().flatMap { verse ->
            verse.tokens.map { token->
                tokens.indexOfFirst { it.token == token}
            }.toList() + listOf(0)
        }.toList()
        val compressedData = encodeIntegers(fileData)
        //println("Book $bookNumber has ${bookVerses.size} verses, ${distinctTokensInBookSorted.size} distinctTokens and can be written in ${compressedData.size} bytes")
        return compressedData
    }

    //Option 3: Adaptive
    private fun bookCompresssion3(
        bookVerses: List<TokenizedVerse>,
        tokens: List<VerseStatsLexiconEntry>,
        bookNumber: Int
    ): ByteArray {
        val fileData: List<Int> = bookVerses.asSequence().flatMap { verse ->
            verse.tokens.map { token->
                tokens.indexOfFirst { it.token == token}
            }.toList() + listOf(0)
        }.toList()
        val byteOutput = encodeWithAdaptiveHuffman(fileData)

        return byteOutput.toByteArray()
    }

    private fun encodeWithAdaptiveHuffman(fileData: List<Int>): ByteArrayOutputStream {
        val byteOutput = ByteArrayOutputStream()
        val bitOutput = BitOutputStream(byteOutput)
        AdaptiveHuffmanCompress.compress(fileData, fileData.maxOrNull() ?: 0, bitOutput)
        bitOutput.close()
        return byteOutput
    }

    //Option 1: Copmact the integer range
    private fun bookCompresssion1(
        bookVerses: List<TokenizedVerse>,
        tokens: List<VerseStatsLexiconEntry>,
        bookNumber: Int
    ): ByteArray {
        val distinctTokensInBook: Set<String> = bookVerses.flatMap { it.tokens }.distinct().toSet()
        val distinctTokensInBookSorted = tokens.filter { distinctTokensInBook.contains(it.token) }.map { it.token }
        val fileData: List<Int> = bookVerses.asSequence().flatMap { verse ->
            verse.tokens.map {
                distinctTokensInBookSorted.indexOf(it) + 1
            }.toList() + listOf(0)
        }.toList()
        val compressedData = encodeIntegers(fileData)
        //println("Book $bookNumber has ${bookVerses.size} verses, ${distinctTokensInBookSorted.size} distinctTokens and can be written in ${compressedData.size} bytes")
        return compressedData
    }

    private fun stopwordTest(stopWordFile: List<Int>) {
        val maxPairsToKeep = 2
        val top = stopWordFile.zipWithNext { a, b -> a to b }.groupingBy { it }.eachCount().toList()
            .sortedByDescending { it.second }.take(maxPairsToKeep)
        val ngrams = top.map { it.first }
        val mapped = ngrams.mapIndexed { index, pair -> pair to (index) }.toMap()
        println(top)

        var i = 0
        val out = mutableListOf<Int>()
        while (i < stopWordFile.size) {
            val currentCode = stopWordFile[i]
            if (i < stopWordFile.size-1) {
                val nextCode = stopWordFile[i + 1]
                val ng = currentCode to nextCode
                val m = mapped[ng]
                if (m != null) {
                    out.add(m)
                    i++
                } else {
                    out.add(currentCode+maxPairsToKeep)
                }
            } else {
                out.add(currentCode+maxPairsToKeep)
            }
            i++
        }
        val compressed = encodeIntegers(out)
        println("Using ngrams, we can get the stop word list to ${compressed.size}")
    }

    private fun testSkips(skipTries: Int, searchFile: MutableList<Int>) {
        val searchDistributions = IntArray(skipTries)
        val optimized = mutableListOf<Int>()
        val minChars = 2
        searchFile.forEachIndexed { index, code ->
            var skipFound = false
            l@ for (i in minChars until (searchDistributions.size + minChars)) {
                if (code > 1 && index + i < searchFile.size && searchFile[index + i] == code) {
                    val skipCode = i - minChars
                    optimized.add(skipCode + 1)
                    searchDistributions[skipCode]++
                    skipFound = true
                    break@l
                }
            }
            if (!skipFound) {
                optimized.add((searchDistributions.size - minChars) + code)
            }
        }
        val optimizedBytes = encodeIntegers(optimized)
        println("Using ${searchDistributions.size} skips, We might be able to save ${searchDistributions.sum()}. Optimized: ${optimizedBytes.size}")
    }

    private fun encodeBits(toggle: List<Int>): ByteArray {
        val byteOut = ByteArrayOutputStream()
        val bitOut = BitOutputStream(byteOut)
        toggle.forEach {
            when (it) {
                0 -> bitOut.writeBit(0)
                1 -> bitOut.writeBit(1)
            }
        }
        bitOut.close()
        return byteOut.toByteArray()
    }

    private fun encodeIntegers(tokens: List<Int>): ByteArray {
        val tokenFreqs = IntArray((tokens.maxOrNull() ?: 0) + 1)
        Arrays.fill(tokenFreqs, 0)
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
        /*
        Write a single lexicon file. A 0 separator means the word belongs in the search file. A 0x01 separator means the word belongs in the stop-word file
        For now, we will just use the same format as version 2
         */
        val highestCharacterCode = lexicon.getTokens().asSequence()
            .flatMap { it.token.toCharArray().asSequence() }
            .maxByOrNull { it }!!
        val initFreqs = IntArray(highestCharacterCode.toInt() + 1)
        Arrays.fill(initFreqs, 0)
        lexicon.getTokens().forEach { token ->
            token.token.chars().forEach { char ->
                initFreqs[char]++
            }
            val isStopWord = stopWords.contains(token.token.toLowerCase())
            if (isStopWord) {
                initFreqs[0]++
            } else {
                initFreqs[1]++
            }

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

            val isStopWord = stopWords.contains(token.token.toLowerCase())
            if (isStopWord) {
                encoder.write(0)
            } else {
                encoder.write(1)
            }
            totalChars++
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

        val headerBytes = CanonicalCodeIO.write(canonCode, bitOutput)
        //println("$headerBytes header bytes written")

        return canonCode.toCodeTree()
    }
}