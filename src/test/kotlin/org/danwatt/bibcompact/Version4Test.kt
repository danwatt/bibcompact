package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat

class Version4Test : VersionBaseTest(Version4Writer(),Version4Reader()) {
    override fun assertTokenWriting(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("008402ffc2cf809f02fff029cbb8")
    }

    override fun assertLexiconWrite(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("0000001e008103801f413ba017b11971b7b11b1b003cbdd80450b151ae6451918ca3f573ef637d4000000002000201f0c0")
    }

    override fun assertHeader(headerBytes: ByteArray) {
        assertThat(headerBytes.toHex()).isEqualTo(
            "02" + // 2 books
                    "0201" + //book 1 has 2 chapters, book 2 has 1 chapter
                    "030102" // chapter 1 has 3 verses, chapter 2 has 1, chapter 3 has 2
        )
    }

    override fun assertFullSimpleWrite(stats: Map<String, Int>, byteOutput: ByteArray) {
        assertThat(stats)
            .containsEntry("headerBytes", 6)
            .containsEntry("lexiconBytes", 66)
            .containsEntry("textBytes", 22)
            .containsEntry("tokens", 6)

        //
        assertThat(byteOutput.toHex()).isEqualTo(
            "04" + //Version number
                    "020201030102" +//Book/Chapter/Verse header
                    "0000002c0081038c88e88e814700a01bdd011e804f446dc75ec6c46e6c600a0156942caf9fd0a2a8a2acb86e5ba6eddf1b787ed508dfd1120000000006000401af78" +//Lexicon
                    "000703babccba0c252612f4c25f987a9372526e4bd00"//Text
        )
    }

    override fun assertKjvWriteStats(stats: Map<String, Int>, rawByte: ByteArray) {
        assertThat(stats)
            .containsEntry("headerBytes", 1256)
            .containsEntry("lexiconBytes", 33499)//v3: 38495 = 4,822 savings
            .containsEntry("textBytes", 999941)//v2: 999812, +643
            .containsEntry("tokens", 13600)
        assertThat(rawByte).hasSize(1_034_697)
    }
}