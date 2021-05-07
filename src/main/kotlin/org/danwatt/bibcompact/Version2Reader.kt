package org.danwatt.bibcompact

import org.danwatt.bibcompact.huffman.BitInputStream
import org.danwatt.bibcompact.huffman.CanonicalCodeIO
import org.danwatt.bibcompact.huffman.HuffmanDecoder
import java.io.InputStream

class Version2Reader : BibReader(2) {
    override fun readVerses(
        input: InputStream,
        counts: List<List<Int>>,
        lex: Lexicon<TokenOnlyEntry>
    ): List<Verse> {
        val endOfVerseMarker = lex.getTokens().size
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
                            tokens.add(lex.getTokens()[t].token)
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
        val totalWords = bitInput.readBits(16)
        val codeTree = CanonicalCodeIO.read(bitInput).toCodeTree()
        val decoder = HuffmanDecoder(bitInput, codeTree)
        var currentWord = ""
        val words = mutableListOf<String>()
        while (words.size < totalWords) {
            val characterCode = decoder.read()
            if (characterCode == 0) {
                words.add(currentWord)
                currentWord = ""
                //End of a word
            } else {
                currentWord += characterCode.toChar()
            }
        }
        bitInput.finishByte()

        return Lexicon.buildFromWordList(words)
    }
}