package org.danwatt.bibcompact

import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.ByteArrayOutputStream


class CompressionUtils {

    companion object {
        fun compress(algo: String, lexBytes: ByteArray): ByteArray {
            val b = ByteArrayOutputStream()
            val out = CompressorStreamFactory(true, 1000).createCompressorOutputStream(algo, b)
            out.write(lexBytes)
            out.flush()
            out.close()
            return b.toByteArray()
        }
    }
}


