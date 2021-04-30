package org.danwatt.bibcompact

object Lzw {
    /** Compress a string to a list of output symbols. */
    fun compress(uncompressed: String): MutableList<Int> {
        // Build the dictionary.
        var dictSize = 256
        val dictionary = mutableMapOf<String, Int>()
        (0 until dictSize).forEach { dictionary[it.toChar().toString()] = it }

        var w = ""
        val result = mutableListOf<Int>()
        for (c in uncompressed) {
            val wc = w + c
            w = if (dictionary.containsKey(wc))
                wc
            else {
                result.add(dictionary[w]!!)
                // Add wc to the dictionary.
                dictionary[wc] = dictSize++
                c.toString()
            }
        }

        // Output the code for w
        if (w.isNotEmpty()) result.add(dictionary[w]!!)
        return result
    }

    /** Decompress a list of output symbols to a string. */
    fun decompress(compressed: MutableList<Int>): String {
        // Build the dictionary.
        var dictSize = 256
        val dictionary = mutableMapOf<Int, String>()
        (0 until dictSize).forEach { dictionary[it] = it.toChar().toString() }

        var w = compressed.removeAt(0).toChar().toString()
        val result = StringBuilder(w)
        for (k in compressed) {
            var entry: String = when {
                dictionary.containsKey(k) -> dictionary[k]!!
                k == dictSize -> w + w[0]
                else -> throw IllegalArgumentException("Bad compressed k: $k")
            }
            result.append(entry)

            // Add w + entry[0] to the dictionary.
            dictionary[dictSize++] = w + entry[0]
            w = entry
        }
        return result.toString()
    }
}