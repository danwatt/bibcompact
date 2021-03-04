package org.danwatt

import java.nio.charset.Charset

class BibleCsvParser {

    fun readTranslation(translation: String): List<Verse> {
        val stream = BibleCsvParser::class.java.getResourceAsStream("/t_${translation}.csv")
        stream.bufferedReader(Charset.forName("UTF-8")).use {
            return it.readLines().asSequence().drop(1).map { line ->
                val s = line.split(",", ignoreCase = true, limit = 5)
                Verse(s[0].toInt(), s[1].toInt(), s[2].toInt(), s[3].toInt(), s[4].trim('"'))
            }.toList()
        }
    }
}