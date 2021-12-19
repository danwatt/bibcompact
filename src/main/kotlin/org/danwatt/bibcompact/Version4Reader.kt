package org.danwatt.bibcompact

import org.danwatt.bibcompact.huffman.BitInputStream
import org.danwatt.bibcompact.huffman.CanonicalCodeIO
import org.danwatt.bibcompact.huffman.HuffmanDecoder
import org.danwatt.bibcompact.radixtree.PrefixTreeReader
import java.io.InputStream
import java.util.Comparator

class Version4Reader : Version3Reader(4) {

    override fun readLexicon(inputStream: InputStream): Lexicon<TokenOnlyEntry> {
        val bitInput = BitInputStream(inputStream)
        val prefixTreeBytes = bitInput.readBits(24)
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