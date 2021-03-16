package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat
import org.danwatt.bibcompact.BibleCsvParser
import org.junit.Test

class BibleCsvParserTest {

    @Test
    fun readJkv() {
        val verses = BibleCsvParser().readTranslation("kjv")
        assertThat(verses).hasSize(31103)
        assertThat(verses[0].text).isEqualTo("In the beginning God created the heaven and the earth.")
        assertThat(verses[1].text).isEqualTo("And the earth was without form, and void; and darkness was upon the face of the deep. And the Spirit of God moved upon the face of the waters.")
    }
}