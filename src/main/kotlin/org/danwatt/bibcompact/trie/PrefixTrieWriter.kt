package org.danwatt.bibcompact.trie

import java.util.*

class PrefixTrieWriter {

    fun write(incomingWords: Collection<String>, suffixes: List<String> = emptyList()): List<Char> {
        val suffixMapping = suffixes.mapIndexed { index, s -> s to 1.shl(index) }.toMap()
        val stack = Stack<Char>()
        val outputList = mutableListOf<Char>()
        suffixes.forEach { suffix ->
            outputList.addAll(suffix.asSequence())
            outputList.add(Char(128))
        }
        val sortedIncoming = incomingWords.toSortedSet()
        val remainingWords = sortedIncoming.toMutableList()
        while (remainingWords.isNotEmpty()) {
            val word = remainingWords.removeFirst()
            var workingWord = word
            var topWord = stack.joinToString("")
            var backspaces = 0
            while (!workingWord.startsWith(topWord) && topWord.isNotEmpty()) {
                stack.pop()
                topWord = topWord.substring(0, topWord.length - 1)
                if (outputList.isNotEmpty() && outputList.last() == WORD_MARKER) {
                    outputList.removeLast()
                }
                backspaces++
            }
            if (backspaces > 0) {
                if (backspaces > 30) {
                    throw IllegalArgumentException("Could not add $backspaces backspaces")
                }
                outputList.add(Char(backspaces))
            }
            if (workingWord.startsWith(topWord)) {
                workingWord = workingWord.removePrefix(topWord)
            }
            workingWord.toCharArray().forEach { char ->
                stack.push(char)
                outputList.add(char)
            }
            var suffixChar = Char(128)

            for ((suffix, char) in suffixMapping) {
                val candidate = word + suffix
                if (sortedIncoming.contains(candidate)) {
                    val foundAt = remainingWords.indexOf(candidate)
                    if (foundAt >= 0) {
                        suffixChar = Char(suffixChar.code + char)
                        remainingWords.removeAt(foundAt)
                    }
                }
            }
            if (suffixChar.code > 128) {
                outputList.add(suffixChar)
            } else {
                outputList.add(WORD_MARKER)
            }
        }
        if (outputList.last() == WORD_MARKER) {
            outputList.removeLast()
        }
        return outputList
    }

    companion object {
        val WORD_MARKER = Char(31)//31 = UNIT SEPARATOR
    }
}