package org.danwatt.bibcompact


import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream

class ByteUtilsKtTest {

    @Test
    fun varByte() {
        assertThat(0.toVarByte()).containsExactly(0b00000000)
        assertThat(1.toVarByte()).containsExactly(0b00000001)
        assertThat(2.toVarByte()).containsExactly(0b00000010)
        assertThat(4.toVarByte()).containsExactly(0b00000100)
        assertThat(8.toVarByte()).containsExactly(0b00001000)
        assertThat(16.toVarByte()).containsExactly(0b00010000)
        assertThat(32.toVarByte()).containsExactly(0b00100000)
        assertThat(64.toVarByte()).containsExactly(0b01000000)

        assertThat((128).to16bitString()).isEqualTo("0000000010000000")
        /*
        127:   0111 1111              1000 0000  0000 0000
        128:   1000 0000              1000 0000  0000 0001
        129:   1000 0001              1000 0001  0000 0001
        130:   1000 0010              1000 0010  0000 0001
        131:   1000 0011              1000 0011  0000 0001
        256:   0000 0001  0000 0000   1000 0000  0000 0010
        257:   0000 0001  0000 0001   1000 0001  0000 0010
        512:   0000 0010  0000 0000   1000 0000  0000 0100
        1024:  0000 0100  0000 0000   1000 0000  0000 1000
        2048:  0000 1000  0000 0000   1000 0000  0001 0000
        4096:  0001 0000  0000 0000   1000 0000  0010 0000
        8192:  0010 0000  0000 0000   1000 0000  0100 0000
        16384: 0100 0000  0000 0000   1000 0000  1000 0000
        32767: 0111 1111  1111 1111   1111 1111  1111 1111
         */
        assertThat(128.toVarByte()).containsExactly(0b1000_0000.toByte(), 0b0000_0001)
        assertThat(129.toVarByte()).containsExactly(0b1000_0001.toByte(), 0b0000_0001)
        assertThat(130.toVarByte()).containsExactly(0b1000_0010.toByte(), 0b0000_0001)
        assertThat(256.toVarByte()).containsExactly(0b1000_0000.toByte(), 0b0000_0010)
        assertThat(257.toVarByte()).containsExactly(0b1000_0001.toByte(), 0b0000_0010)
        assertThat(512.toVarByte()).containsExactly(0b1000_0000.toByte(), 0b0000_0100)
        assertThat(1024.toVarByte()).containsExactly(0b1000_0000.toByte(), 0b0000_1000)
        assertThat(2048.toVarByte()).containsExactly(0b1000_0000.toByte(), 0b0001_0000)
        assertThat(4096.toVarByte()).containsExactly(0b1000_0000.toByte(), 0b0010_0000)
        assertThat(8192.toVarByte()).containsExactly(0b1000_0000.toByte(), 0b0100_0000)
        assertThat(16384.toVarByte()).containsExactly(0b1000_0000.toByte(), 0b1000_0000.toByte())
        assertThat(32767.toVarByte()).containsExactly(0b1111_1111.toByte(), 0b1111_1111.toByte())
    }

    @Test
    fun readVarByte() {
        assertThat(ByteArrayInputStream(64.toVarByte().toByteArray()).readVarByteInt()).isEqualTo(64)
        assertThat(ByteArrayInputStream(128.toVarByte().toByteArray()).readVarByteInt()).isEqualTo(128)
        assertThat(ByteArrayInputStream(32767.toVarByte().toByteArray()).readVarByteInt()).isEqualTo(32767)
    }
}
