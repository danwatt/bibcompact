package org.danwatt.bibcompact.trie

import java.util.*

class PrefixTrieReader {
    fun read(characters: List<Char>): List<String> {
        val words = mutableListOf<String>()
        val suffixes = mutableListOf<String>()
        val stack = LinkedList<Char>()
        characters.forEach { c ->
            when {
                c == WORD_MARKER -> words.add(stack.joinToString(""))
                c == SUFFIX_MARKER -> {
                    suffixes.add(stack.joinToString(""))
                    stack.clear()
                }
                c.code > SUFFIX_MARKER.code -> {
                    val word = stack.joinToString("")
                    for (i in (0 until suffixes.size)) {
                        if (c.code.and(1.shl(i)) > 0) {
                            words.add(word + suffixes[i])
                        }
                    }
                }
                c.code < WORD_MARKER.code -> {
                    words.add(stack.joinToString(""))
                    repeat(c.code) { stack.removeLast() }
                }
                else -> stack.add(c)
            }
        }

        words.add(stack.joinToString(""))
        return words
    }

    companion object {
        val WORD_MARKER = Char(31)//31 = UNIT SEPARATOR
        val SUFFIX_MARKER = Char(128)
    }
}