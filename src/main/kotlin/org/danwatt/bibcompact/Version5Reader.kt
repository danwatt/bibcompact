package org.danwatt.bibcompact

import org.danwatt.bibcompact.huffman.BitInputStream
import org.danwatt.bibcompact.huffman.CanonicalCodeIO
import org.danwatt.bibcompact.huffman.HuffmanDecoder
import org.danwatt.bibcompact.trie.PrefixTrieReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.Exception
import java.util.Comparator

class Version5Reader : Version3Reader(5) {

    /* The big difference here is that version 5 has two lexicons - one for stop words, one for significant words */

    override fun read(input: InputStream): List<Verse> {
        val versionNumber = input.read()
        if (versionNumber != this.version) {
            throw IllegalArgumentException("Bad version number encountered, expected ${this.version} but was $versionNumber")
        }
        val counts = readHeader(input)
        val stopWordLexicon = readLexicon(input)
        val significantWordLexicon = readLexicon(input)
        return readVerses(input, counts, stopWordLexicon, significantWordLexicon)
    }

    /* We have two separate files to read:
search word file
stop word file

The search word file has the end of verse markers
The stop word file has placeholders for search words
 */

    fun readVerses(
        input: InputStream,
        counts: List<List<Int>>,
        stopWordLexicon: Lexicon<TokenOnlyEntry>,
        searchWordLexicon: Lexicon<TokenOnlyEntry>,
    ): List<Verse> {

        val inputAsByteArray = input.readBytes()
        val bitMappingFileLength = inputAsByteArray.read32bitInt(0)
        val stopWordFileLength = inputAsByteArray.read32bitInt(4)
        val searchWordFileLength = inputAsByteArray.read32bitInt(8)

        val endOfBitMappingFile = 12 + bitMappingFileLength
        val endOfStopWordFile = endOfBitMappingFile + stopWordFileLength
        val bitMappingFile = inputAsByteArray.copyOfRange(12, endOfBitMappingFile)
        val stopWordFile = inputAsByteArray.copyOfRange(endOfBitMappingFile, endOfStopWordFile)
        val searchWordFile = inputAsByteArray.copyOfRange(endOfStopWordFile, endOfStopWordFile + searchWordFileLength)

        val bitInput = BitInputStream(ByteArrayInputStream(bitMappingFile))

        /* Read the stop word file. Note that it does NOT have verse markers */
        val stopWordTokens = decodeFile(stopWordFile)

        /* Read the search word file */
        val searchWordTokens = decodeFile(searchWordFile)

        /* Combine the two files */
        val words = mutableListOf<String>()
        var currentStopWord = 0
        var currentSearchWord = 0

        val endOfVerseMarker = 0

        val verses = mutableListOf<Verse>()
        for (b in counts.indices) {
            for (c in counts[b].indices) {
                for (v in 0 until counts[b][c]) {
                    val tokens = mutableListOf<String>()
                    //We will always end on a stop word, as the final code should be an EOV marker
                    while (currentStopWord < stopWordTokens.size) {
                        when (val bit = bitInput.readBit()) {
                            0 -> {//We have a stop word / EOV marker
                                val stopWordToken = stopWordTokens[currentStopWord++]
                                if (stopWordToken == endOfVerseMarker) {
                                    break
                                }
                                tokens.add(stopWordLexicon.getTokens()[stopWordToken - 1].token)
                            }
                            1 -> {
                                val searchWordToken = searchWordTokens[currentSearchWord++]
                                tokens.add(searchWordLexicon.getTokens()[searchWordToken].token)
                            }
                            else -> {
                                throw IllegalArgumentException("Unknown bit: $bit")
                            }
                        }
                    }
                    verses.add(applyEnglishLanguageFixesAndBuildVerse(tokens, b, c, v))
                }
            }
        }

        return verses
    }

    private fun decodeFile(stopWordFile: ByteArray): List<Int> {
        val bitInput = BitInputStream(stopWordFile.inputStream())
        val codeTree = CanonicalCodeIO.read(bitInput).toCodeTree()
        val decoder = HuffmanDecoder(bitInput, codeTree)

        val l = mutableListOf<Int>()
        while (true) {
            try {
                //TODO: Something is wrong with readAll()
                l.add(decoder.read())
            } catch (ex: Exception) {
                break
            }
        }
        return l

    }

    override fun readLexicon(inputStream: InputStream): Lexicon<TokenOnlyEntry> {
        val bitInput = BitInputStream(inputStream)
        val words = readPrefixTree(bitInput)
        val c: Comparator<String> = compareBy<String> { words[it] }.thenComparing { it -> it }
        val wordsSorted = words.keys.map { it }.sortedWith(c).toList()

        return Lexicon.buildFromWordList(wordsSorted)
    }


    private fun readPrefixTree(bitInput: BitInputStream): Map<String, Int> {
        val prefixTreeCodeLength = bitInput.readBits(32)

        val codeTree = CanonicalCodeIO.read(bitInput).toCodeTree()
        val decoder = HuffmanDecoder(bitInput, codeTree)
        val prefixTreeCodes = mutableListOf<Char>()
        for (i in 0 until prefixTreeCodeLength) {
            prefixTreeCodes.add(decoder.read().toChar())
        }
        bitInput.finishByte()
        val tree = PrefixTrieReader().read(prefixTreeCodes)

        val bitAllotmentCodeLength = bitInput.readBits(32)
        if (bitAllotmentCodeLength > 0) {
            val allotmentCodeTree = CanonicalCodeIO.read(bitInput).toCodeTree()
            val allotmentDecoder = HuffmanDecoder(bitInput, allotmentCodeTree)
            val allotments = mutableListOf<Int>()
            for (i in 0 until bitAllotmentCodeLength) {
                allotments.add(allotmentDecoder.read())
            }
            bitInput.finishByte()
            return tree.mapIndexed { index, word -> word to allotments[index] }.toMap()
        } else {
            return emptyMap()
        }

    }

}