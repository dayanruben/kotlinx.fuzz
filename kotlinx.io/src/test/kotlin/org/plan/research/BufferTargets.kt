package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import org.plan.research.utils.*
import kotlin.reflect.KCallable
import kotlin.test.assertEquals

object BufferTargets {
    val funs = ReflectionUtils.bufferFunctions + Buffer::toString
    val funs_stable = funs.filter { it.name != "readAtMostTo" }.toTypedArray()

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun randomOps(data: FuzzedDataProvider): Unit = with(data) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)
        val buf = Buffer().apply { write(initBytes) }
        val n = consumeInt(0, Constants.MAX_OPERATIONS_NUMBER)
        doNOps(n, data, buf)
    }


    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun copyTest(data: FuzzedDataProvider): Unit = with(data) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)
        val buf = Buffer().apply { write(initBytes) }
        val n = consumeInt(0, Constants.MAX_OPERATIONS_NUMBER / 2)
        val ops = doNOps(n, data, buf)
        val copy = buf.copy()
        template(buf, copy, data, funs_stable) { defaultParams(data) }
        assertEquals(buf.toString(), copy.toString())
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun writeDecimalLong(data: FuzzedDataProvider): Unit = with(data) {
        val buf = Buffer()
        val secBuf = buf.copy()
//        buf.snapshot().let { s -> Buffer().apply {  }
        val n = consumeInt(0, Constants.MAX_OPERATIONS_NUMBER)
        repeat(n) {
            val long = consumeLong()
            buf.writeDecimalLong(long)
            secBuf.writeString(long.toString())
            assertEquals(buf.snapshot(), secBuf.snapshot())
            val v = buf.readDecimalLong()
            assertEquals(long, v)
            buf.clear()
            secBuf.clear()
            assertEquals(ByteString(byteArrayOf()), buf.snapshot())
            assertEquals(ByteString(byteArrayOf()), secBuf.snapshot())
        }
    }

    private fun FuzzedDataProvider.doNOps(
        n: Int,
        data: FuzzedDataProvider,
        buf: Buffer
    ): Pair<List<KCallable<*>>, List<*>> {
        val ops = mutableListOf<KCallable<*>>()
        val results = mutableListOf<Any?>()
        repeat(n) {
            val op = pickValue(funs)
            ops += op

            val args = op.generateArguments(data) { defaultParams(data) }
            results += catching { op.call(buf, *args) }
        }
        return ops to results
    }

}