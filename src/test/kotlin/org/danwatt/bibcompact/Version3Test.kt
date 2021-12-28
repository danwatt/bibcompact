package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.*

class Version3Test : VersionBaseTest(Version3Writer(), Version3Reader()) {
    override fun assertTokenWriting(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("008402ffc2cf809f02fff029cbb8")
    }

    override fun assertLexiconWrite(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("00000b0077038b828e402f2222e00b7005b018e2e0dea900")
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
                    "00001900750388cb816680a01bbb011d804ec00ae2362360057b1798b45c60652f0d779fa2c6cb930" +//Lexicon
                    "0000703babccba0c252612f4c25f987a9372526e4bd00"//Text
        )
    }

    override fun assertKjvWriteStats(stats: Map<String, Int>, rawByte: ByteArray) {
        assertThat(stats)
            .containsEntry("headerBytes", 1256)
            .containsEntry("lexiconBytes", 38394)//v2: 61085, -22,589
            .containsEntry("textBytes", 999941)//v2: 999812, +643
            .containsEntry("tokens", 13600)
        assertThat(rawByte).hasSize(1_039_592)//less than 1MB!!!!!!
    }

}