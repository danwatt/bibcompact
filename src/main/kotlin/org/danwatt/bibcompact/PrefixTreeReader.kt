package org.danwatt.bibcompact

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory
import org.danwatt.bibcompact.PrefixTreeWriter.Companion.POP_CODE
import org.danwatt.bibcompact.PrefixTreeWriter.Companion.PUSH_CODE
import java.util.*

class PrefixTreeReader {
    fun read(codes: List<Int>): ConcurrentRadixTree<Int> {
        val words = mutableListOf<String>()
        val stack = Stack<String>()
        var i = 0
        var currentWord = ""
        var lastPop = ""
        var lastCode = -1
        val tree = ConcurrentRadixTree<Int>(DefaultCharArrayNodeFactory())
        while (i < codes.size) {
            val indent = "\t".repeat(stack.size)
            val currentCode = codes[i]
            when {
                currentCode < PUSH_CODE -> {
                    if (currentWord == "") {
                        currentWord = lastPop
                    }
                    tree.put(stack.joinToString("") + currentWord, currentCode)
                }
                currentCode == PUSH_CODE -> {
                    stack.push(currentWord)
                    currentWord = ""
                }
                currentCode == POP_CODE -> {
                    if (currentWord != "") {
                        words.add(stack.joinToString("") + currentWord)
                    }
                    lastPop = stack.pop()
                    currentWord = ""
                }
                else -> {
                    if (lastCode < PUSH_CODE) {
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
        return tree
    }

}