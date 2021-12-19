package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat

class Version2Test : VersionBaseTest(Version2Writer(), Version2Reader()) {
    override fun assertTokenWriting(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("008403bbbb83ddddd8053977")
    }

    override fun assertLexiconWrite(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("00020077049c0a54802e908427002ce005980ce2e0dea900")
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
            .containsEntry("lexiconBytes", 44)
            .containsEntry("textBytes", 23)
            .containsEntry("tokens", 6)

        //
        assertThat(byteOutput.toHex()).isEqualTo(
            "02" + //Version number
                    "020201030102" +//Book/Chapter/Verse header
                    "00060075058a017ca02801b2cb0046580132c0028e082582096001492c11c920b1988f1aef3f499b4e517300" +//Lexicon
                    "00070494e73a52408530c29b98537b0bd867530cea6e40"//Text
        )
    }

    override fun assertKjvWriteStats(stats: Map<String, Int>, rawByte: ByteArray) {
        assertThat(stats)
            .containsEntry("headerBytes", 1256)
            .containsEntry("lexiconBytes", 61085)
            .containsEntry("textBytes", 999812)
            .containsEntry("tokens", 13600)
        assertThat(rawByte).hasSize(1_062_154)//SO close to 1,048,576 (1MB) - 13,578 bytes
    }
}