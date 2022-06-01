package org.danwatt.bibcompact

import org.danwatt.bibcompact.Version3Writer.Companion.buildCodeLengthMapping
import org.danwatt.bibcompact.huffman.BitOutputStream
import org.danwatt.bibcompact.trie.PrefixTrieWriter
import java.io.ByteArrayOutputStream
import kotlin.Comparator

class Version5Writer(val stopWords: Set<String>) : BibWriter(5) {

    override fun writeVerseData(verses: List<TokenizedVerse>, lexicon: Lexicon<VerseStatsLexiconEntry>): ByteArray {

        val END_VERSE_MARKER = 0

        val (stopWords, searchWords) = lexicon.getTokens()
            .partition { stopWords.contains(it.token.lowercase()) }

        val stopWordsByFrequency = convertLexiconToListSortedByCode(stopWords)
            .mapIndexed { index, s -> s to index }
            .toMap()
        val searchWordsByFrequency = convertLexiconToListSortedByCode(searchWords)
            .mapIndexed { index, s -> s to index }
            .toMap()

        val searchFile = mutableListOf<Int>()
        val stopWordFile = mutableListOf<Int>()
        val byteOut = ByteArrayOutputStream()
        val bitMapping = BitOutputStream(byteOut)

        verses.asSequence().forEach { verse ->
            verse.tokens.forEach { token: String ->
                when {
                    stopWordsByFrequency.containsKey(token) -> {
                        val sw = (stopWordsByFrequency[token]!! + 1)
                        bitMapping.writeBit(0)
                        stopWordFile.add(sw)
                    }
                    else -> {
                        val sw = searchWordsByFrequency[token]!!
                        bitMapping.writeBit(1)
                        searchFile.add(sw)
                    }
                }
            }
            bitMapping.writeBit(0)
            stopWordFile.add(END_VERSE_MARKER)
        }
        bitMapping.close()

        val stopWordFileBytes = writeHuffmanWithTree(stopWordFile)
        val searchFileBytes = writeHuffmanWithTree(searchFile)

        val bitMappingFileBytes = byteOut.toByteArray()

        return bitMappingFileBytes.size.to4ByteArray() +
                stopWordFileBytes.size.to4ByteArray() +
                searchFileBytes.size.to4ByteArray() +
                bitMappingFileBytes +
                stopWordFileBytes +
                searchFileBytes
    }

    private fun convertLexiconToListSortedByCode(tokens: List<VerseStatsLexiconEntry>): List<String> {
        val tokenToCodeLength = buildCodeLengthMapping(tokens)
        val c: Comparator<String> = compareBy<String> { token -> tokenToCodeLength[token] }.thenComparing { it -> it }
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
        writeBitAllocations(byteOutput, lexicon, sortedLexicon)

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
            val bitMapping: Map<String, Int> = buildCodeLengthMapping(lexicon)
            val bitAllotments = sortedLexicon.mapNotNull { bitMapping[it] }.toList()
            sortedLexicon.filter { str -> bitMapping[str]!! > 18 }.forEach { str ->
                println("$str needs ${bitMapping[str]} bits")
            }
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