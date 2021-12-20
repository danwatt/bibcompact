package org.danwatt.bibcompact

import org.danwatt.bibcompact.huffman.BitInputStream
import org.danwatt.bibcompact.huffman.CanonicalCodeIO
import org.danwatt.bibcompact.huffman.HuffmanDecoder
import org.danwatt.bibcompact.radixtree.PrefixTreeReader
import java.io.InputStream
import java.util.Comparator

open class Version3Reader(version: Int = 3) : BibReader(version) {
    override fun readVerses(
        input: InputStream,
        counts: List<List<Int>>,
        lex: Lexicon<TokenOnlyEntry>
    ): List<Verse> {
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
                            tokens.add(lex.getTokens()[t - 1].token)
                        }
                    }
                    verses.add(applyEnglishLanguageFixesAndBuildVerse(tokens, b, c, v))
                    counter++
                }
            }
        }
        return verses
    }

    override fun readLexicon(inputStream: InputStream): Lexicon<TokenOnlyEntry> {
        val bitInput = BitInputStream(inputStream)
        val prefixTreeBytes = bitInput.readBits(24)
        println("Reading ${prefixTreeBytes} bytes of prefix tree data")
        val codeTree = CanonicalCodeIO.read(bitInput).toCodeTree()
        val decoder = HuffmanDecoder(bitInput, codeTree)
        val prefixTreeCodes = mutableListOf<Int>()
        for (i in 0 until prefixTreeBytes) {
            prefixTreeCodes.add(decoder.read())
        }
        bitInput.finishByte()

        val tree = PrefixTreeReader().read(prefixTreeCodes)
        val keys = tree.getKeysStartingWith("")
        val decoded = mutableMapOf<String, Int>()
        keys.map {
            decoded.put(it.toString(), tree.getValueForExactKey(it.toString()))
        }

        val c: Comparator<String> = compareBy<String> { decoded[it] }.thenComparing { it -> it }
        val wordsSorted = decoded.keys.map { it }.sortedWith(c).toList()

        println("Found ${wordsSorted.size} words in ${prefixTreeBytes}")
        return Lexicon.buildFromWordList(wordsSorted)
    }
}