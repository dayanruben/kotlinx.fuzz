package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.Buffer
import kotlinx.io.asOutputStream
import kotlinx.io.bytestring.asReadOnlyByteBuffer
import kotlinx.io.readByteArray
import kotlinx.io.readIntLe
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

object Temp {
    @FuzzTest
    fun lol(data: FuzzedDataProvider) = with(data) {
        val buf = kotlinx.io.Buffer()
        val bytes = data.consumeBytes(1000)
        buf.write(bytes)
        val got = buf.readByteArray(bytes.size)
        buf.equals(buf)
        assertContentEquals(bytes, got)
    }

    @FuzzTest
    fun writeToBuffer(data: FuzzedDataProvider) = with(data) {
        val bytes = data.consumeRemainingAsBytes()
        val fromBuf = Buffer().apply { write(bytes) }
        val toBuff = Buffer()
        toBuff.write(fromBuf, bytes.size.toLong())
        assertContentEquals(bytes, toBuff.readByteArray())
    }

    @FuzzTest
    fun hz(data: FuzzedDataProvider) = with(data) {
        val buf = Buffer()
        buf.asOutputStream()
    }
}