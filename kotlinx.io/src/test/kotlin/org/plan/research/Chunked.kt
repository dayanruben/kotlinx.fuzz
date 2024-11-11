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
import kotlin.random.Random
import kotlin.reflect.KFunction

object Chunked {
    fun getChunk(n: Int): Array<KFunction<*>> =
        ReflectionUtils.sourceFunctions.toList().shuffled(Random(n)).take(20).toTypedArray()


    val functions: Array<Array<KFunction<*>>> = Array(5) { i -> getChunk(i) }

    inline fun curTemp(data: FuzzedDataProvider, functionsNumber: Int) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)

        val source = initBytes.inputStream().asSource().buffered()
        val buf: Source = Buffer().apply { write(initBytes) }

        data.template(source, buf, data, functions[functionsNumber])
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun t0(data: FuzzedDataProvider): Unit = with(data) {
        curTemp(data, 0)
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun t1(data: FuzzedDataProvider): Unit = with(data) {
        curTemp(data, 1)
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun t2(data: FuzzedDataProvider): Unit = with(data) {
        curTemp(data, 2)
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun t3(data: FuzzedDataProvider): Unit = with(data) {
        curTemp(data, 3)
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun t4(data: FuzzedDataProvider): Unit = with(data) {
        curTemp(data, 4)
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun t5(data: FuzzedDataProvider): Unit = with(data) {
        curTemp(data, 5)
    }
}