package org.danwatt.bibcompact

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.ByteBuffer

class BinaryTest {
    @Test
    fun testBinary() {
        val bb = ByteBuffer.allocate(32)
        bb.putChar(0,(127).toChar())
        bb.put(2,'A'.toByte())

        assertThat(bb.array()).containsSequence(0,127,65)
    }
}