package org.danwatt.bibcompact

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree
import com.googlecode.concurrenttrees.radix.node.Node

class PrefixTreeWriter {
    companion object {
        const val WORD_MARKER = 0
        const val PUSH_CODE = 1
        const val POP_CODE = 2
    }

    fun write(tree: ConcurrentRadixTree<*>): List<Int> {
        val list = mutableListOf<Int>()

        tree.node.outgoingEdges.forEach { edge ->
            encodeTree(edge, list, 0)
        }
        return list
    }


    fun encodeTree(
        node: Node,
        list: MutableList<Int>,
        depth: Int
    ) {

        val prefix = "\t".repeat(depth)
        val children = node.outgoingEdges
        debug(
            "${prefix}Working on ${node.incomingEdge}. Children : ${children.size} : ${
                children.joinToString(", ") { it.incomingEdge }
            }"
        )
        val incoming = node.incomingEdge.toString()

        list.addAll(node.incomingEdge.map { it.toInt() })

        if (children.isNotEmpty()) {
            debug("${incoming}Pushing")
            list.add(PUSH_CODE)
        }
        children.sortedBy { it.incomingEdge.toString() }
            .forEach { child ->
                debug("${incoming}\tEncoding child ${child.incomingEdge}")
                encodeTree(child as Node, list, depth + 1)
            }
        if (children.isNotEmpty()) {
            debug("${incoming}Popping")
            list.add(POP_CODE)
        }

        if (node.value != null) {
            debug("${incoming}Adding word marker to ${node.incomingEdge}")
            list.add(WORD_MARKER)
        } else {
            debug("${incoming}Skipping word marker for ${node.incomingEdge}")
        }
    }

    private fun debug(s: String) {
        //println(s)
    }
}