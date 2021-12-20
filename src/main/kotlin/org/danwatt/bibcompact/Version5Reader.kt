package org.danwatt.bibcompact

import org.danwatt.bibcompact.huffman.BitInputStream
import org.danwatt.bibcompact.huffman.CanonicalCodeIO
import org.danwatt.bibcompact.huffman.HuffmanDecoder
import java.io.InputStream

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
        val stopWordFileLength = inputAsByteArray.read32bitInt(0)
        val searchWordFileLength = inputAsByteArray.read32bitInt(4)

        /* Read the stop word file. Note that it does NOT have verse markers */

        /* Read the search word file */

        /* Combine the two files */

        val endOfVerseMarker = 0
        val bitInput = BitInputStream(input)
        val codeTree = CanonicalCodeIO.read(bitInput).toCodeTree()
        val decoder = HuffmanDecoder(bitInput, codeTree)
        var counter = 1
        val verses = mutableListOf<Verse>()
        for (b in counts.indices) {
            for (c in counts[b].indices) {
                for (v in 0 until counts[b][c]) {
                    val tokens = mutableListOf<String>()
                    var t = -1
                    while (t != endOfVerseMarker) {
                        t = decoder.read()
                        if (t != endOfVerseMarker) {
                            tokens.add(stopWordLexicon.getTokens()[t - 1].token)
                        }
                    }
                    verses.add(applyEnglishLanguageFixesAndBuildVerse(tokens, b, c, v))
                    counter++
                }
            }
        }
        return verses
    }

}