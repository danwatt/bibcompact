package org.danwatt.bibcompact

import java.util.*


/*
 * This is a Java implementation of a deterministic acyclic finite
 * state automaton (DAFSA) data structure used for storing a finite
 * number of strings in a space-efficient way.  It constructs and
 * initially populates itself using a dictionary text file.
 *
 * This data structure supports a query operation that runs in time
 * proportional to the number of characters in the query.
 * Read http://en.wikipedia.org/wiki/Deterministic_acyclic_finite_state_automaton
 * for more information.
 *
 * Command-line arguments are parsed as filepaths to text files containing
 * words to add into the data structure.
 *
 * @author David Weinberger (davidtweinberger@gmail.com)
 * Adapted from http://stevehanov.ca/blog/index.php?id=115
 */

/*
 * This is a Java implementation of a deterministic acyclic finite
 * state automaton (DAFSA) data structure used for storing a finite
 * number of strings in a space-efficient way.  It constructs and
 * initially populates itself using a dictionary text file.
 *
 * This data structure supports a query operation that runs in time
 * proportional to the number of characters in the query.
 * Read http://en.wikipedia.org/wiki/Deterministic_acyclic_finite_state_automaton
 * for more information.
 *
 * Command-line arguments are parsed as filepaths to text files containing
 * words to add into the data structure.
 *
 * @author David Weinberger (davidtweinberger@gmail.com)
 * Adapted from http://stevehanov.ca/blog/index.php?id=115
 */
class DAFSA {
    private var _previousWord = ""
    private val _root: DAFSA_Node

    //list of nodes that have not been checked for duplication
    private val _uncheckedNodes: ArrayList<Triple>

    //list of unique nodes that have been checked for duplication
    private val _minimizedNodes: HashSet<DAFSA_Node>

    //A class representing an immutable 3-tuple of (node, character, node)
    private inner class Triple(val node: DAFSA_Node, val letter: Char, val next: DAFSA_Node)

    //A (static) class representing a node in the data structure.
    private class DAFSA_Node {
        //mutators
        //accessors
        //instance variables
        var id: Int
        var final: Boolean
        val edges: HashMap<Char?, DAFSA_Node>

        override fun equals(obj: Any?): Boolean {
            if (this === obj) {
                return true
            }
            if (obj == null) {
                return false
            }
            val other = obj as DAFSA_Node
            return id == other.id && final === other.final && edges === other.edges
        }

        /*
		@Override
		public int hashCode(){
			int hash = 1;
			hash += 17*_id;
			hash += 31*_final.hashCode();
			hash += 13*_edges.hashCode();
			return hash;
		}
		*/
        //representation of this node as a string
        override fun toString(): String {
            val sb = StringBuilder()
            if (final) {
                sb.append("1")
            } else {
                sb.append("0")
            }
            for ((key, value) in edges) {
                sb.append("_")
                sb.append(key)
                sb.append("_")
                sb.append(value.id)
            }
            return sb.toString()
        }

        //add edges to the hashmap
        fun addEdge(letter: Char?, destination: DAFSA_Node) {
            edges[letter] = destination
        }

        fun containsEdge(letter: Char?): Boolean {
            return edges.containsKey(letter)
        }

        fun traverseEdge(letter: Char?): DAFSA_Node? {
            return edges[letter]
        }

        fun numEdges(): Int {
            return edges.size
        }

        companion object {
            //class variables
            private var currentID = 0
        }

        init {
            id = currentID
            currentID++
            final = false
            edges = HashMap()
        }
    }

    fun insert(word: String) {
        //if word is alphabetically before the previous word
        if (_previousWord > word) {
            System.err.println("Inserted in wrong order:$_previousWord, $word")
            return
        }

        //find the common prefix between word and previous word
        var prefix = 0
        val len = word.length.coerceAtMost(_previousWord.length)
        for (i in 0 until len) {
            if (word[i] != _previousWord[i]) {
                break
            }
            prefix += 1
        }

        //check the unchecked nodes for redundant nodes, proceeding from
        //the last one down to the common prefix size.  Then truncate the list at that point.
        minimize(prefix)

        //add the suffix, starting from the correct node mid-way through the graph
        var node: DAFSA_Node
        node = if (_uncheckedNodes.size == 0) {
            _root
        } else {
            _uncheckedNodes[_uncheckedNodes.size - 1].node
        }
        val remainingLetters = word.substring(prefix) //the prefix+1th character to the end of the string
        for (element in remainingLetters) {
            val nextNode = DAFSA_Node()
            node.addEdge(element, nextNode)
            _uncheckedNodes.add(Triple(node, element, nextNode))
            node = nextNode
        }
        node.final = true
        _previousWord = word
    }

    private fun minimize(downTo: Int) {
        // proceed from the leaf up to a certain point
        for (i in _uncheckedNodes.size - 1 downTo downTo) {
            val t = _uncheckedNodes[i]
            val iter: Iterator<DAFSA_Node> = _minimizedNodes.iterator()
            var foundMatch = false
            while (iter.hasNext()) {
                val match = iter.next()
                if (t.next == match) {
                    //replace the child with the previously encountered one
                    t.node.addEdge(t.letter, t.next)
                    foundMatch = true
                    break
                }
            }
            if (!foundMatch) {
                _minimizedNodes.add(t.next)
            }
            _uncheckedNodes.removeAt(i)
        }
    }

    operator fun contains(word: String): Boolean {
        var node: DAFSA_Node? = _root
        var letter: Char
        for (element in word) {
            letter = element
            node = if (!node!!.containsEdge(letter)) {
                return false
            } else {
                node.traverseEdge(letter)
            }
        }
        return node!!.final
    }

    fun nodeCount(): Int {
        //counts nodes
        return _minimizedNodes.size
    }

    fun edgeCount(): Int {
        //counts edges
        var count = 0
        val iter: Iterator<DAFSA_Node> = _minimizedNodes.iterator()
        var curr: DAFSA_Node
        while (iter.hasNext()) {
            curr = iter.next()
            count += curr.numEdges()
        }
        return count
    }

    fun finish() {
        minimize(0)
    }

    init {
        _root = DAFSA_Node()
        _uncheckedNodes = ArrayList()
        _minimizedNodes = HashSet() //TODO type
    }
}