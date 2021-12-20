package org.danwatt.bibcompact

import org.danwatt.bibcompact.huffman.BitInputStream
import org.danwatt.bibcompact.huffman.CanonicalCodeIO
import org.danwatt.bibcompact.huffman.HuffmanDecoder
import org.danwatt.bibcompact.trie.PrefixTrieReader
import java.io.InputStream
import java.util.Comparator

class Version4Reader : Version3Reader(4) {

    /* Lexicon format:
4 bytes (B): The number of codepoints in the Trie
B codepoints : the trie structure itself
4 bytes (C): The number of items in the lexicon
C codepoints : the bit allocations of each word (canonicalized huffman)
 */
    override fun readLexicon(inputStream: InputStream): Lexicon<TokenOnlyEntry> {
        val bitInput = BitInputStream(inputStream)
        val words = readPrefixTree(bitInput)
        val c: Comparator<String> = compareBy<String> { words[it] }.thenComparing { it -> it }
        val wordsSorted = words.keys.map { it }.sortedWith(c).toList()

        return Lexicon.buildFromWordList(wordsSorted)
    }


    private fun readPrefixTree(bitInput: BitInputStream): Map<String, Int> {
        val prefixTreeCodeLength = bitInput.readBits(32)

        println("Lexicon has ${prefixTreeCodeLength} huffman codes")
        val codeTree = CanonicalCodeIO.read(bitInput).toCodeTree()
        val decoder = HuffmanDecoder(bitInput, codeTree)
        val prefixTreeCodes = mutableListOf<Char>()
        for (i in 0 until prefixTreeCodeLength) {
            prefixTreeCodes.add(decoder.read().toChar())
        }
        bitInput.finishByte()
        val tree = PrefixTrieReader().read(prefixTreeCodes)
        println("Prefix tree has ${tree.size} items in it")

        val bitAllotmentCodeLength = bitInput.readBits(32)
        println("Bit allotment has ${bitAllotmentCodeLength} huffman codes")
        val allotmentCodeTree = CanonicalCodeIO.read(bitInput).toCodeTree()
        val allotmentDecoder = HuffmanDecoder(bitInput, allotmentCodeTree)
        val allotments = mutableListOf<Int>()
        for (i in 0 until bitAllotmentCodeLength) {
            allotments.add(allotmentDecoder.read())
        }
        bitInput.finishByte()
        return tree.mapIndexed { index, word -> word to allotments[index] }.toMap()
    }
}