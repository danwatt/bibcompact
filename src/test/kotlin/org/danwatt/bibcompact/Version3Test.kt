package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.*

class Version3Test : VersionBaseTest(Version3Writer(), Version3Reader()) {
    override fun assertTokenWriting(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("008403bbb82cdc026e02f77629cbb8")
    }

    override fun assertLexiconWrite(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("00000b00770484270285200ba42109c00b38016603e2e0dea900")
    }

    override fun assertHeader(headerBytes: ByteArray) {
        assertThat(headerBytes.toHex()).isEqualTo(
            "02" + // 2 books
                    "0201" + //book 1 has 2 chapters, book 2 has 1 chapter
                    "030102" // chapter 1 has 3 verses, chapter 2 has 1, chapter 3 has 2
        )
    }

    override fun assertFullSimpleWrite(stats: Map<String, Int>, byteOutput: ByteArray) {
        assertThat(byteOutput.toHex()).isEqualTo(
            "03" + //Version number
                    "020201030102" +//Book/Chapter/Verse header
                    "000019007505800524700ae501400d96580232c00996001470412c104b000a39608e4900b45c60652f0d779fa2c6cb930" +//Lexicon
                    "00007049ca74a4e40c252612f4c25f987a9372526e4bd00"//Text
        )
    }

    override fun assertKjvWriteStats(stats: Map<String, Int>, rawByte: ByteArray) {
        assertThat(stats)
            .containsEntry("headerBytes", 1256)
            .containsEntry("lexiconBytes", 38495)//v2: 61085, -22,589
            .containsEntry("textBytes", 1000455)//v2: 999812, +643
            .containsEntry("tokens", 13600)
        assertThat(rawByte).hasSize(1_040_207)//8,369 bytes under 1MB!!!!!!
    }

}