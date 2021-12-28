package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat

class Version2Test : VersionBaseTest(Version2Writer(), Version2Reader()) {
    override fun assertTokenWriting(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("008402fff87bfff0053977")
    }

    override fun assertLexiconWrite(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("0002007703b8296402f2222e00b7005b0180e2e0dea900")
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
            .containsEntry("lexiconBytes", 38)
            .containsEntry("textBytes", 22)
            .containsEntry("tokens", 6)

        //
        assertThat(byteOutput.toHex()).isEqualTo(
            "02" + //Version number
                    "020201030102" +//Book/Chapter/Verse header
                    "0006007503a817e80a01bbb011d804ec00ae2362360059b17980b1988f1aef3f499b4e517300" +//Lexicon
                    "000703abbbcca08530c29b98537b0bd867530cea6e40"//Text
        )
    }

    override fun assertKjvWriteStats(stats: Map<String, Int>, rawByte: ByteArray) {
        assertThat(stats)
            .containsEntry("headerBytes", 1256)
            .containsEntry("lexiconBytes", 60993)
            .containsEntry("textBytes", 999677)
            .containsEntry("tokens", 13600)
        assertThat(rawByte).hasSize(1_061_927)//SO close to 1,048,576 (1MB)
    }
}