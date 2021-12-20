package org.danwatt.bibcompact

import org.danwatt.bibcompact.Version3Writer.Companion.buildBitMapping
import org.danwatt.bibcompact.huffman.BitOutputStream
import org.danwatt.bibcompact.trie.PrefixTrieWriter
import java.io.ByteArrayOutputStream

class Version5Writer(val stopWords: Set<String>) : BibWriter(5) {

    override fun writeVerseData(verses: List<TokenizedVerse>, lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {

        val SEARCH_WORD_PLACEHOLDER = 0
        val END_VERSE_MARKER = 1

        val (stopWords, searchWords) = lexicon.getTokens()
            .partition { stopWords.contains(it.token.lowercase()) }

        val stopWordsByFrequency: Map<String, Int> =
            convertLexiconToList(
                stopWords,
                listOf(Integer.MAX_VALUE, Integer.MAX_VALUE - 1)
            ).mapIndexed { index, s -> s to index }.toMap()
        val searchWordsByFrequency: Map<String, Int> =
            convertLexiconToList(searchWords, listOf()).mapIndexed { index, s -> s to index }.toMap()

        val searchFile = mutableListOf<Int>()
        val stopWordFile = mutableListOf<Int>()

        verses.asSequence().forEach { verse ->
            verse.tokens.forEach { token: String ->
                when {
                    stopWordsByFrequency.containsKey(token) -> {
                        val sw = (stopWordsByFrequency[token]!! + 2)
                        stopWordFile.add(sw)
                    }
                    else -> {
                        stopWordFile.add(SEARCH_WORD_PLACEHOLDER)//Indicates that this word is in the search file
                        val sw = searchWordsByFrequency[token]!!
                        searchFile.add(sw)
                    }
                }
            }
            stopWordFile.add(END_VERSE_MARKER)
        }


        val stopWordFileBytes = writeHuffmanWithTree(stopWordFile)
        val searchFileBytes = writeHuffmanWithTree(searchFile)

        return stopWordFileBytes.size.to4ByteArray() +
                searchFileBytes.size.to4ByteArray() +
                stopWordFileBytes +
                searchFileBytes
    }

    private fun convertLexiconToList(tokens: List<VerseStatsLexiconEntry>, reservedCodes: List<Int>): List<String> {
        val bitMapping = buildBitMapping(reservedCodes, tokens)
        val c: Comparator<String> = compareBy<String> { bitMapping[it] }.thenComparing { it -> it }
        return tokens.map { it.token }.sortedWith(c).toList()
    }


    override fun writeLexicon(lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {

        val (stopTokens, searchTokens) = lexicon.getTokens().partition { stopWords.contains(it.token.lowercase()) }

        val stopBytes = writeV4Lexicon(stopTokens)
        val searchBytes = writeV4Lexicon(searchTokens)

        return stopBytes + searchBytes
    }

    private fun writeV4Lexicon(lexicon: List<VerseStatsLexiconEntry>): ByteArray {
        val sortedLexicon = lexicon.map { it.token }.toSortedSet()
        val byteOutput = ByteArrayOutputStream()

        writePrefixTrie(byteOutput, sortedLexicon)
        val trieSize = byteOutput.size()
        writeBitAllocations(byteOutput, lexicon, sortedLexicon)
        val allocationSize = byteOutput.size() - trieSize

        return byteOutput.toByteArray()
    }

    private fun writeBitAllocations(
        byteOutput: ByteArrayOutputStream,
        lexicon: List<VerseStatsLexiconEntry>,
        sortedLexicon: Collection<String>
    ) {
        //It is possible to have a 0 length lexicon
        val wordBitMappingOutput = BitOutputStream(byteOutput)
        if (lexicon.isEmpty()) {
            wordBitMappingOutput.writeBits(0, 32)
        } else {
            val bitMapping = buildBitMapping(listOf(Integer.MAX_VALUE), lexicon)
            val bitAllotments = sortedLexicon.mapNotNull { bitMapping[it] }.toList()
            wordBitMappingOutput.writeBits(bitAllotments.size, 32)
            writeHuffmanWithTree(wordBitMappingOutput, bitAllotments)
        }
        wordBitMappingOutput.close()
    }

    private fun writePrefixTrie(
        byteOutput: ByteArrayOutputStream,
        sortedLexicon: Collection<String>
    ) {
        val trieChars: List<Char> = PrefixTrieWriter().write(
            sortedLexicon,
            //TODO: Make this dynamic
            listOf("s", "ed", "ing", "eth", "th", "h", "d")
        )

        val lexiconBitOutput = BitOutputStream(byteOutput)
        lexiconBitOutput.writeBits(trieChars.size, 32)
        writeHuffmanWithTree(lexiconBitOutput, trieChars.map { it.code })
        lexiconBitOutput.close()
    }

}