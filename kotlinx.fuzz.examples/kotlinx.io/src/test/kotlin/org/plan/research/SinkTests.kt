@file:Suppress("UNUSED_VARIABLE")

package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.Buffer
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import org.plan.research.utils.*
import org.plan.research.utils.ReflectionUtils.sinkFunctions
import java.io.ByteArrayOutputStream
import kotlin.reflect.KFunction
import kotlin.test.assertContentEquals

object SinkTests {
    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun randomOpsVsBuffer(data: FuzzedDataProvider): Unit = localTemplate(data, sinkFunctions)

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun largeDataVsBuffer(data: FuzzedDataProvider): Unit = with(data) {
        val source = SystemFileSystem.source(Path("data.bin")).buffered()

        val ostream = ByteArrayOutputStream()
        val sink = ostream.asSink().buffered()
        val buf = Buffer()

        val ops = mutableListOf<Long>()
        repeat(consumeInt(0, Constants.MAX_OPERATIONS_NUMBER)) {
            val byteCount = consumeLong(0, Long.MAX_VALUE)
            ops += byteCount
            val peek = source.peek()

            val r1 = catching { peek.readTo(sink, byteCount) }
            val r2 = catching { source.readTo(buf, byteCount) }
            assertEqualsComplete(r1, r2)
        }

        sink.flush()
        val sinkRes = ostream.toByteArray()
        val bufRes = buf.readByteArray()
        assertContentEquals(sinkRes, bufRes)
    }

    private val chunks = sinkFunctions.splitIntoChunks(6, 20)

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun ch0(data: FuzzedDataProvider) = localTemplate(data, chunks[0])

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun ch1(data: FuzzedDataProvider) = localTemplate(data, chunks[1])

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun ch2(data: FuzzedDataProvider) = localTemplate(data, chunks[2])

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun ch3(data: FuzzedDataProvider) = localTemplate(data, chunks[3])

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun ch4(data: FuzzedDataProvider) = localTemplate(data, chunks[4])

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun ch5(data: FuzzedDataProvider) = localTemplate(data, chunks[5])

    private fun localTemplate(
        data: FuzzedDataProvider, functions: Array<KFunction<*>>
    ) {
        val ostream = ByteArrayOutputStream()
        val sink = ostream.asSink().buffered()
        val buf = Buffer()

        val ops = template(sink, buf, data, functions) { defaultParams(data) }
        sink.flush()
        val sinkRes = ostream.toByteArray()
        val bufRes = buf.readByteArray()
        assertContentEquals(sinkRes, bufRes)
    }
}

