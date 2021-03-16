package org.danwatt.bibcompact.futurework

import java.util.*

data class CondensedVerse (
    val numTokens: Int,
    val fileSelector: BitSet,
    val masterSubfile: IntArray,
    val textFile: IntArray
)