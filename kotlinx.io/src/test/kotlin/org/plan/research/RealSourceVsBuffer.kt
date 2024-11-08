package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.plan.research.utils.Couple
import org.plan.research.utils.ReflectionUtils
import org.plan.research.utils.copyArguments
import org.plan.research.utils.generateArguments
import org.plan.research.utils.template
import kotlin.random.Random
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter.Kind

object RealSourceVsBuffer {
    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun randomOps(data: FuzzedDataProvider): Unit = with(data) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)

        val source = initBytes.inputStream().asSource().buffered()
        val buf: Source = Buffer().apply { write(initBytes) }

        template(source, buf, data, ReflectionUtils.sourceFunctions)
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun onlyExtensions(data: FuzzedDataProvider): Unit = with(data) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)

        val source = initBytes.inputStream().asSource().buffered()
        val buf: Source = Buffer().apply { write(initBytes) }

        val funs = ReflectionUtils.sourceFunctions
            .filter { it.parameters.first().kind == Kind.EXTENSION_RECEIVER }
            .toTypedArray()

        template(source, buf, data, funs)
    }

    val batches = ReflectionUtils.sourceFunctions
        .toList()
        .sortedBy { it.name }
        .shuffled(Random(42))
        .chunked(5)
        .map { it.toTypedArray() }
        .toTypedArray()

    @FuzzTest(maxExecutions = 1)
    fun oaoaoa(data: FuzzedDataProvider): Unit = with(data) {
        val r = java.security.SecureRandom()
        println(r.nextLong())
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun batched(data: FuzzedDataProvider): Unit = with(data) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)

        val source = initBytes.inputStream().asSource().buffered()
        val buf: Source = Buffer().apply { write(initBytes) }

        val couple = Couple(source, buf)
        val ops = mutableListOf<KCallable<*>>()
        val n = consumeInt(0, 100)

        repeat(n) {
            for (op in ReflectionUtils.sourceFunctions) {
                ops += op

                val args = op.generateArguments(data)
                val args2 = op.copyArguments(args, data)
                couple.invokeOperation<Any?>(op, args, args2)
            }
        }
    }

    val visited = hashMapOf<KCallable<*>, Int>()

}


