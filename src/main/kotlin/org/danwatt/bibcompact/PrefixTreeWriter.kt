package org.danwatt.bibcompact

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree
import com.googlecode.concurrenttrees.radix.node.Node

class PrefixTreeWriter {
    companion object {
        const val WORD_MARKER = 0
        const val PUSH_CODE = 1
        const val POP_CODE = 2
//        const val WORD_MARKER_CAP = 2
//        const val WORD_MARKER_UPPER = 4
    }

    fun write(tree: ConcurrentRadixTree<*>): List<Int> {
        val list = tree.node.outgoingEdges.flatMap { encodeTree(it, 0) }.toList()
        return cleanup(list)
    }

    private fun cleanup(list: List<Int>): List<Int> {
        var i = list.size - 1
        var numToRemove = 0
        while (list[i] == 2) {
            numToRemove++
            i--
        }
        return list.subList(0, list.size - numToRemove)
    }


    fun encodeTree(
        node: Node,
        depth: Int
    ): List<Int> {
        val list = mutableListOf<Int>()
        val children = node.outgoingEdges
        node.incomingEdge.toString()

        list.addAll(node.incomingEdge.map { it.toInt() })
        if (node.value != null) {
            //var marker = node.value as Int
            list.add(0)
        }

        if (children.isNotEmpty()) {
            list.add(PUSH_CODE)
        }
        children.sortedBy { it.incomingEdge.toString() }
            .forEach { child ->
                list.addAll(encodeTree(child as Node, depth + 1))
            }
        if (children.isNotEmpty()) {
            //A word marker can be implied, so long as it is not immediately preceeded by a POP
            if (list.last() == WORD_MARKER && list[list.size - 2] != POP_CODE) {
                list.removeLast()
            }
            list.add(POP_CODE)
        }
        return list
    }
}