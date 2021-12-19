package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat

class Version4Test : VersionBaseTest(Version4Writer(),Version4Reader()) {
    override fun assertTokenWriting(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("008403bbb82cdc026e02f77629cbb8")
    }

    override fun assertLexiconWrite(bytes: ByteArray) {
        assertThat(bytes.toHex()).isEqualTo("000200810680038d009d1a002e2c081121c08b0e2c081160458003890e2c58008840b151ae6451918ca3f573ef637d40000301ecc0")
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
            .containsEntry("lexiconBytes", 75)
            .containsEntry("textBytes", 23)
            .containsEntry("tokens", 6)

        //
        assertThat(byteOutput.toHex()).isEqualTo(
            "04" + //Version number
                    "020201030102" +//Book/Chapter/Verse header
                    "000600810681120408d0204680288c02800d8d1a004634004c681022c3811a1c581160408b122c08802800a860942caf9fd0a2a8a2acb86e5ba6eddf1b787ed508dfd11200000501abc078" +//Lexicon
                    "0007049ca74a4e40c252612f4c25f987a9372526e4bd00"//Text
        )
    }

    override fun assertKjvWriteStats(stats: Map<String, Int>, rawByte: ByteArray) {
        assertThat(stats)
            .containsEntry("headerBytes", 1256)
            .containsEntry("lexiconBytes", 33673)//v3: 38495 = 4,822 savings
            .containsEntry("textBytes", 1000455)//v2: 999812, +643
            .containsEntry("tokens", 13600)
        assertThat(rawByte).hasSize(1_035_385)
    }
}