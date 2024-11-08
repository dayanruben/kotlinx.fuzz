package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.Buffer
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.io.write
import org.plan.research.SinkTests.toB
import org.plan.research.utils.ReflectionUtils
import org.plan.research.utils.defaultParams
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals

object SinkTests {
    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun aa(data: FuzzedDataProvider): Unit = with(data) {
        val ostream = ByteArrayOutputStream()
        val sink = ostream.asSink().buffered()
        val buf = Buffer()

        val ops = template(sink, buf, data, ReflectionUtils.sinkFunctions) { defaultParams(data) }
        sink.flush()
        val sinkRes = ostream.toByteArray()//.toB()
        val bufRes = buf.readByteArray()//.toB()
        assertContentEquals(sinkRes, bufRes)
    }

    fun ByteArray.toB(): ByteArray {
        val r1 = indexOfLast { it != 0.toByte() }
        if (r1 == -1) return byteArrayOf()
        return sliceArray(0 until r1)
    }
}

object Aboba {
    @Test
    fun lol() {
        val ostream = ByteArrayOutputStream()
        val sink = ostream.asSink().buffered()
        val buf = Buffer()

        val bb = ByteBuffer.wrap(byteArrayOf(1, 2, 3))
        val bb2 = ByteBuffer.wrap(byteArrayOf(1, 2, 3))

        sink.write(bb)
        buf.write(bb2)

        sink.flush()
        val sinkRes = ostream.toByteArray().toB()
        val bufRes = buf.readByteArray().toB()
        assertContentEquals(sinkRes, bufRes)
    }

}