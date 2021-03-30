package org.danwatt.bibcompact.futurework


sealed class CondenseToken
data class Skip(val placesToSkip: Int) : CondenseToken()
data class Lookup(val tokenNumber: Int) : CondenseToken()

class SkipCondenser {
    fun condense(tokens: List<Int>): List<CondenseToken> {
        val out = mutableListOf<CondenseToken>()
        for (i in tokens.indices) {
            val token = tokens[i]
            val next = tokens.nextIndexOf(token, i + 1)
            if (next == null) {
                out.add(Lookup(token))
            } else {
                out.add(Skip(next - i))
            }
        }
        return out
    }
}

private fun <E> List<E>.nextIndexOf(value: E, startAt: Int): Int? {
    for (i in startAt until this.size) {
        if (this[i] == value) {
            return i
        }
    }
    return null
}
