package org.danwatt.bibcompact

import org.danwatt.bibcompact.huffman.BitOutputStream
import org.danwatt.bibcompact.huffman.CanonicalCode
import org.danwatt.bibcompact.huffman.FrequencyTable
import org.danwatt.bibcompact.trie.PrefixTrieWriter
import java.io.ByteArrayOutputStream

class Version5Writer(val stopWords: Set<String>) : BibWriter(5) {

    override fun writeVerseData(verses: List<TokenizedVerse>, lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {
        val END_VERSE_MARKER = 0
        val SEARCH_WORD_PLACEHOLDER = 0

        val (stopWords, searchWords) = lexicon.getTokens()
            .partition { stopWords.contains(it.token.lowercase()) }

        val stopWordsByFrequency: Map<String, Int> =
            convertLexiconToList(stopWords).mapIndexed { index, s -> s to index }.toMap()
        val searchWordsByFrequency: Map<String, Int> =
            convertLexiconToList(searchWords).mapIndexed { index, s -> s to index }.toMap()

        val searchFile = mutableListOf<Int>()
        val stopWordFile = mutableListOf<Int>()

        verses.asSequence().forEach { verse ->
            verse.tokens.forEach {
                when {
                    stopWordsByFrequency.containsKey(it) -> {
                        stopWordFile.add(stopWordsByFrequency[it]!!)
                    }
                    else -> {
                        stopWordFile.add(SEARCH_WORD_PLACEHOLDER)//Indicates that this word is in the search file
                        searchFile.add(searchWordsByFrequency[it]!!)
                    }
                }
            }
            stopWordFile.add(END_VERSE_MARKER)
        }

        val searchFileBytes = writeHuffmanWithTree(searchFile)
        val stopWordFileBytes = writeHuffmanWithTree(stopWordFile)

        println("Search file :${searchFileBytes.size}")
        println("Stopword file :${stopWordFileBytes.size}")
        println("Total size: ${searchFileBytes.size + stopWordFileBytes.size}")

        return searchFileBytes + stopWordFileBytes
    }

    private fun convertLexiconToList(tokens: List<VerseStatsLexiconEntry>): List<String> {
        val bitMapping = buildBitMapping(tokens)
        val c: Comparator<String> = compareBy<String> { bitMapping[it] }.thenComparing { it -> it }
        return tokens.map { it.token }.sortedWith(c).toList()
    }

    fun buildBitMapping(lexicon: List<VerseStatsLexiconEntry>): Map<String, Int> {
        val tokenFreqs = IntArray(lexicon.size + 1)
        tokenFreqs[0] = Integer.MAX_VALUE
        lexicon.forEachIndexed { index, token -> tokenFreqs[index + 1] = token.totalOccurrences }
        val frequencies = FrequencyTable(tokenFreqs)
        val originalCodeTree = frequencies.buildCodeTree()
        val canonCode = CanonicalCode(originalCodeTree, frequencies.getSymbolLimit())
        val bitMapping = lexicon.mapIndexed { index, token ->
            token.token to canonCode.getCodeLength(index)
        }.toMap()
        return bitMapping
    }

    private fun writeV4Lexicon(tokens: List<VerseStatsLexiconEntry>): ByteArray {
        val sortedLexicon = tokens.map { it.token }
            .toSortedSet()

        val trieChars: List<Char> = PrefixTrieWriter().write(
            sortedLexicon,
            listOf("s", "ed", "ing", "eth", "th", "h", "d")
        )

        val byteOutput = ByteArrayOutputStream()
        val lexiconBitOutput = BitOutputStream(byteOutput)
        lexiconBitOutput.writeBits(sortedLexicon.size, 16)
        writeHuffmanWithTree(lexiconBitOutput, trieChars.map { it.code })
        lexiconBitOutput.close()

        val bitMapping = buildBitMapping(tokens)
        val bitAllotments = sortedLexicon.mapNotNull { bitMapping[it] }.toList()
        val wordBitMappingOutput = BitOutputStream(byteOutput)
        writeHuffmanWithTree(wordBitMappingOutput, bitAllotments)
        wordBitMappingOutput.close()
        return byteOutput.toByteArray()
    }

    override fun writeLexicon(lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {

        val (stop, search) = lexicon.getTokens().partition { stopWords.contains(it.token.lowercase()) }

        val stopBytes = writeV4Lexicon(stop)
        val searchBytes = writeV4Lexicon(search)

        println("Stop word lexicon is ${stopBytes.size} bytes")
        println("Search word lexicon is ${searchBytes.size} bytes")

        return stopBytes + searchBytes
    }

    fun compressStopWordListing(stopWordList: List<Int>): List<Int> {
        return stopWordList

    }
}