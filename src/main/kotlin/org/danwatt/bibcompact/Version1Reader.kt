package org.danwatt.bibcompact

import java.io.InputStream

class Version1Reader : BibReader(1) {

    override fun readVerses(input: InputStream, counts: List<List<Int>>, lex: Lexicon<TokenOnlyEntry>): List<Verse> {
        var counter = 1
        val verses = mutableListOf<Verse>()
        for (b in counts.indices) {
            for (c in counts[b].indices) {
                for (v in 0 until counts[b][c]) {
                    val numTokens = input.read()
                    val tokens = mutableListOf<String>()
                    for (t in 0 until numTokens) {
                        tokens.add(lex.getTokens()[input.readVarByteInt()].token)
                    }
                    verses.add(applyEnglishLanguageFixesAndBuildVerse(tokens, b, c, v))
                    counter++
                }
            }
        }
        return verses
    }

    override fun readLexicon(inputStream: InputStream): Lexicon<TokenOnlyEntry> {
        val numTokens = inputStream.readInt()
        val tokens = mutableListOf<TokenOnlyEntry>()

        for (t in 0 until numTokens) {
            var c: Int = -1
            val currentToken = StringBuffer()
            while (c != 0) {
                c = inputStream.read()
                if (c != 0) {
                    currentToken.append(c.toChar())
                } else {
                    tokens.add(TokenOnlyEntry(token = currentToken.toString()))
                }
            }
        }

        return Lexicon(tokens)

    }
}

private fun InputStream.readInt() = this.read().shl(8) + this.read()
