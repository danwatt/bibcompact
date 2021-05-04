package org.danwatt.bibcompact

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory
import org.danwatt.bibcompact.PrefixTreeWriter.Companion.POP_CODE
import org.danwatt.bibcompact.PrefixTreeWriter.Companion.PUSH_CODE
import org.danwatt.bibcompact.PrefixTreeWriter.Companion.WORD_MARKER
import java.util.*

class PrefixTreeReader {
    fun read(bytes: List<Int>): ConcurrentRadixTree<Boolean> {
        val words = mutableListOf<String>()
        val stack = Stack<String>()
        var i = 0
        var currentWord = ""
        var lastPop = ""
        while (i < bytes.size) {
            val indent = "\t".repeat(stack.size)
            when (val currentCode = bytes[i]) {
                WORD_MARKER -> {
                    if (currentWord == "") {
                        currentWord = lastPop
                    }
                    words.add(stack.joinToString("") + currentWord)
                    currentWord = ""
                }
                PUSH_CODE -> {
                    debug("${indent}Pushing $currentWord on to {${stack.joinToString(", ")}}")
                    stack.push(currentWord)
                    currentWord = ""
                }
                POP_CODE -> {
                    lastPop = stack.pop()
                    debug("${"\t".repeat(stack.size)}Popped $lastPop, stack now {${stack.joinToString(", ")}}")
                    currentWord = ""
                }
                else -> {
                    currentWord += currentCode.toChar()
                }
            }
            i++
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

    private fun debug(s: String) {
        //println(s)
    }
}