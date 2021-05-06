package org.danwatt.bibcompact

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory
import org.danwatt.bibcompact.PrefixTreeWriter.Companion.POP_CODE
import org.danwatt.bibcompact.PrefixTreeWriter.Companion.PUSH_CODE
import org.danwatt.bibcompact.PrefixTreeWriter.Companion.WORD_MARKER
import java.util.*

class PrefixTreeReader {
    fun read(codes: List<Int>): ConcurrentRadixTree<Boolean> {
        val words = mutableListOf<String>()
        val stack = Stack<String>()
        var i = 0
        var currentWord = ""
        var lastPop = ""
        var lastCode = -1
        while (i < codes.size) {
            val indent = "\t".repeat(stack.size)
            val currentCode = codes[i]
            when (currentCode) {
                WORD_MARKER -> {
                    if (currentWord == "") {
                        currentWord = lastPop
                    }
                    words.add(stack.joinToString("") + currentWord)
                }
                PUSH_CODE -> {
                    stack.push(currentWord)
                    currentWord = ""
                }
                POP_CODE -> {
                    if (currentWord != "") {
                        words.add(stack.joinToString("") + currentWord)
                    }
                    lastPop = stack.pop()
                    currentWord = ""
                }
                else -> {
                    if (lastCode == WORD_MARKER) {
                        currentWord = ""
                    }
                    currentWord += currentCode.toChar()
                }

            }
            lastCode = currentCode
            i++
        }
        if (currentWord != "") {
            words.add(stack.joinToString("") + currentWord)
        }
        val tree = ConcurrentRadixTree<Boolean>(DefaultCharArrayNodeFactory())
        words.forEach {
            //HMMM
            if (it.isNotEmpty()) {
                tree.put(it, true)
            }
        }
        return tree
    }

}