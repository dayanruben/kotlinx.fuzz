package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.plan.research.Constants.MAX_DURATION
import org.plan.research.Constants.MAX_OPERATIONS_NUMBER
import kotlin.test.assertEquals

object BuffersKt {
    val manyBytes = SystemFileSystem.source(Path("data.bin")).buffered().readByteString()

    val arr = ByteArray(manyBytes.size * 100)

    @FuzzTest(maxDuration = MAX_DURATION)
    fun snapshot(data: FuzzedDataProvider): Unit = with(data) {
        val buf = Buffer()
        val n = consumeInt(0, MAX_OPERATIONS_NUMBER)
        var written = 0
        repeat(n) {
            val from = consumeInt(0, manyBytes.size - 10)
            val to = consumeInt(from, manyBytes.size)
            val len = to - from
            buf.write(manyBytes, from, to)

            if (written + len > arr.size) return

            manyBytes.copyInto(arr, written, from, to)
            written += len
            assertEquals(buf.snapshot(), ByteString(arr).substring(0, written))
        }
    }

    val buf = Buffer().apply { write(manyBytes) }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun indexOfByte(data: FuzzedDataProvider): Unit = with(data) {
        val b = consumeByte()
        buf.indexOf(b)
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun indexOfByteString(data: FuzzedDataProvider): Unit = with(data) {
        val bytes = ByteString(consumeBytes(10))
        buf.indexOf(bytes)
    }
}
