@file:Suppress("NOTHING_TO_INLINE")

package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.plan.research.utils.ReflectionUtils
import org.plan.research.utils.template
import kotlin.reflect.KFunction

object Chunked {
    fun getChunk(n: Int) =
        ReflectionUtils.sourceFunctions.let { it.toList().chunked(it.size / 4) }[n].toTypedArray()

    val ch1 = getChunk(0)

    inline fun curTemp(data: FuzzedDataProvider, funs: Array<KFunction<*>>) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)

        val source = initBytes.inputStream().asSource().buffered()
        val buf: Source = Buffer().apply { write(initBytes) }

        data.template(source, buf, data, funs)
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun t1(data: FuzzedDataProvider): Unit = with(data) {
        curTemp(data, ch1)
    }

    val ch2 = getChunk(1)

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun t2(data: FuzzedDataProvider): Unit = with(data) {
        curTemp(data, ch2)
    }

    val ch3 = getChunk(2)

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun t3(data: FuzzedDataProvider): Unit = with(data) {
        curTemp(data, ch3)
    }

    val ch4 = getChunk(3)

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun t4(data: FuzzedDataProvider): Unit = with(data) {
        curTemp(data, ch4)
    }

    val ch5 = getChunk(4)

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun t5(data: FuzzedDataProvider): Unit = with(data) {
        curTemp(data, ch5)
    }
}