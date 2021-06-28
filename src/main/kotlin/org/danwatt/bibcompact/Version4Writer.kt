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
        //bookCompressionTest(verses, lexicon)
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
                    stopWordFile.add(stopWordList.indexOf(it))
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
        val searchFileBytes = writeHuffmanWithTree(searchFile)
        val stopWordFileBytes = writeHuffmanWithTree(stopWordFile)
        println("Switch file :${switchFileBytes.size}")//118524, or 118528 as Huffman codes
        println("Search file :${searchFileBytes.size}")
        println("Stopword file :${stopWordFileBytes.size}")
        println("Total size: ${searchFileBytes.size + stopWordFileBytes.size}")

        for (i in -16 .. 16) {
            val swc = bigramTest(stopWordFile, 1024 + i)
        }
        println("Search file")
        /*
NGrams: 128. Original: 917089 original compressed: 484905, n-gram uncompressed: 853369, n-gram compressed: 464466. Savings: 20439
NGrams: 256. Original: 917089 original compressed: 484905, n-gram uncompressed: 836796, n-gram compressed: 462483. Savings: 22422
NGrams: 512. Original: 917089 original compressed: 484905, n-gram uncompressed: 818910, n-gram compressed: 460453. Savings: 24452
NGrams: 1024. Original: 917089 original compressed: 484905, n-gram uncompressed: 800197, n-gram compressed: 459146. Savings: 25759
NGrams: 2048. Original: 917089 original compressed: 484905, n-gram uncompressed: 784329, n-gram compressed: 460672. Savings: 24233
NGrams: 4096. Original: 917089 original compressed: 484905, n-gram uncompressed: 776264, n-gram compressed: 468845. Savings: 16060
NGrams: 8192. Original: 917089 original compressed: 484905, n-gram uncompressed: 778115, n-gram compressed: 487230. Savings: -2325
         */


        for (skip in 2..32) {
            testSkips(skip, searchFile)
        }

        //Best so far: 16 skips : 468539, 1024 ngrams: 459146 = 927,685, vs 1,000,455 for V3

        /* Using three files :
        Switch file :118528
        Search file :478561 ->
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

       // bookCompressionTest(verses, lexicon, stopWordList, searchWordList)

        return /*switchFileBytes +*/ searchFileBytes + stopWordFileBytes
    }

    private fun bookCompressionTest(
        verses: List<TokenizedVerse>,
        lexicon: Lexicon<VerseStatsLexiconEntry>,
        stopWordList: List<String>,
        searchWordList: List<String>
    ) {
        val tokens: List<VerseStatsLexiconEntry> = lexicon.getTokens()
        val byBook: Map<Int, List<TokenizedVerse>> = verses.groupBy { it.book }

        var totalBytesCompact = 0
        var totalBytesAdaptive = 0

        byBook.forEach { (bookNumber, bookVerses) ->
            //val compressedBook = bookCompresssionStandard(bookVerses, tokens, bookNumber, stopWordList, searchWordList)
            val compressedData2 = bookCompressCompactIntRange(bookVerses, tokens, bookNumber, lexicon, stopWordList, searchWordList)
            val bookCompressAdaptive = bookCompresssion3(bookVerses, tokens, bookNumber, stopWordList, searchWordList)
            totalBytesCompact += compressedData2.size
            totalBytesAdaptive += bookCompressAdaptive.size

            println("$bookNumber\t${bookCompressAdaptive.size}")
        }
        println("Encoding each book separately is $totalBytesCompact")
        println("Adaptive encoding on each book separately is $totalBytesAdaptive")
    }

    // Option 1 : Normal
    private fun bookCompresssionStandard(
        bookVerses: List<TokenizedVerse>,
        tokens: List<VerseStatsLexiconEntry>,
        bookNumber: Int,
        stopWordList: List<String>,
        searchWordList: List<String>
    ): ByteArray {
        val fileData: List<Int> = bookVerses.asSequence().flatMap { verse ->
            verse.tokens.filter { searchWordList.contains(it) }
                .map { token -> tokens.indexOfFirst { it.token == token } }
                .toList() + listOf(0)
        }.toList()
        val compressedData = writeHuffmanWithTree(fileData)
        //println("Book $bookNumber has ${bookVerses.size} verses, ${distinctTokensInBookSorted.size} distinctTokens and can be written in ${compressedData.size} bytes")
        return compressedData
    }

    //Option 2: Copmact the integer range
    private fun bookCompressCompactIntRange(
        bookVerses: List<TokenizedVerse>,
        tokens: List<VerseStatsLexiconEntry>,
        bookNumber: Int,
        lexicon: Lexicon<VerseStatsLexiconEntry>,
        stopWordList: List<String>,
        searchWordList: List<String>
    ): ByteArray {
        /*
        Find all distinct search words in the book
        Sort them by frequency in the book (or ... just use adative)
        Write out their token numbers in that order, which can be used to build a new Huffman tree
        Encode the book data using that huffman tree
         */
        val s= searchWordList.toSet()
        val distinctTokensInBook: Set<String> =
            bookVerses.flatMap { it.tokens }.distinct().filter { searchWordList.contains(it) }.toSet()
        val distinctTokensInBookSorted = tokens.filter { distinctTokensInBook.contains(it.token) }.map { it.token }
        println("Book $bookNumber has ${distinctTokensInBookSorted.size} distinct search words")

        //First write out the offsets
        val headerData = distinctTokensInBookSorted.map { lexicon.getLookupValue(it)!! + 1 } + listOf(0)

        println("Wrote ${headerData.size} header bytes")

        val fileData: List<Int> = bookVerses.asSequence().flatMap { verse ->
            verse.tokens.map {
                distinctTokensInBookSorted.indexOf(it) + 1
            }.toList() + listOf(0)
        }.toList()
        val compressedData =encodeWithAdaptiveHuffman(fileData)
        //println("Book $bookNumber has ${bookVerses.size} verses, ${distinctTokensInBookSorted.size} distinctTokens and can be written in ${compressedData.size} bytes")
        return compressedData
    }

    //Option 3: Normal but with adaptive huffman
    private fun bookCompresssion3(
        bookVerses: List<TokenizedVerse>,
        tokens: List<VerseStatsLexiconEntry>,
        bookNumber: Int,
        stopWordList: List<String>,
        searchWordList: List<String>
    ): ByteArray {
        val s = searchWordList.toSet()
        val fileData: List<Int> = bookVerses.asSequence()
            .flatMap { verse ->
                verse.tokens.filter { s.contains(it) }
                    .map { token -> tokens.indexOfFirst { it.token == token } }
                    .toList() + listOf(0)
            }.toList()
        val byteOutput = encodeWithAdaptiveHuffman(fileData)

        return byteOutput
    }

    private fun bigramTest(codeList: List<Int>, maxPairsToKeep: Int = 128): ByteArray {
        val top = codeList.filter { it > 0 }
            .zipWithNext { a, b -> a to b }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(maxPairsToKeep)
        val ngrams = top.map { it.first }
        val mapped = ngrams.mapIndexed { index, pair -> pair to (index) }.toMap()

        var i = 0
        val out = mutableListOf<Int>()
        while (i < codeList.size) {
            val currentCode = codeList[i]
            if (i < codeList.size - 1) {
                val nextCode = codeList[i + 1]
                val ng = currentCode to nextCode
                val m: Int? = mapped[ng]
                if (m != null) {
                    out.add(m)
                    i++
                } else {
                    out.add(maxPairsToKeep + currentCode)
                }
            } else {
                out.add(maxPairsToKeep + currentCode)
            }
            i++
        }
        val ngramCodes = listOf(maxPairsToKeep) + mapped.asSequence().sortedBy { it.value }
            .flatMap { listOf(it.key.first, it.key.second) }.toList()

        val finalOutput = ngramCodes + out
        val compressed = writeHuffmanWithTree(finalOutput)
        val originalCompressed = writeHuffmanWithTree(codeList)
        println("BiGram: ${maxPairsToKeep}. Original: ${codeList.size} original compressed: ${originalCompressed.size}, n-gram uncompressed: ${finalOutput.size}, n-gram compressed: ${compressed.size}. Savings: ${originalCompressed.size - compressed.size}")
        return compressed
    }

    private fun testSkips(skipTries: Int, searchFile: MutableList<Int>) {
        val searchDistributions = IntArray(skipTries)
        val optimized = mutableListOf<Int>()
        val minSpaces = 2
        var skipsAdded = 0
        searchFile.forEachIndexed { index, code ->
            var skipFound = false
            l@ for (i in minSpaces until (searchDistributions.size + minSpaces)) {
                if (code > 1 && index + i < searchFile.size && searchFile[index + i] == code) {
                    skipsAdded++
                    val skipCode = i - minSpaces
                    optimized.add(skipCode + 1)
                    searchDistributions[skipCode]++
                    skipFound = true
                    break@l
                }
            }
            if (!skipFound) {
                optimized.add((searchDistributions.size - minSpaces) + code)
            }
        }
        val originalCompressed = writeHuffmanWithTree(searchFile)
        val optimizedBytes = writeHuffmanWithTree(optimized)
        println("Using ${searchDistributions.size} skips (total ${skipsAdded}), we save ${originalCompressed.size - optimizedBytes.size} (end: ${optimizedBytes.size})")
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

        CanonicalCodeIO.write(canonCode, bitOutput)
        //println("$headerBytes header bytes written")

        return canonCode.toCodeTree()
    }
}