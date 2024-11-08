package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.plan.research.RealSourceVsBuffer.visited
import org.plan.research.utils.ReflectionUtils
import org.plan.research.utils.copyArguments
import org.plan.research.utils.generateArguments
import java.lang.reflect.InvocationTargetException
import kotlin.random.Random
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter.Kind
import kotlin.reflect.full.isSubclassOf
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

class Couple<T>(val test: T, val control: T) {
    companion object {
        inline fun <R> runCathing(
            vararg catchingClass: KClass<out Throwable> = arrayOf(
                RuntimeException::class,
                IOException::class,
            ),
            block: () -> R
        ): Result<R> = try {
            Result.success(block())
        } catch (e: InvocationTargetException) {
//            e as InvocationTargetException
            if (catchingClass.none { e.targetException::class.isSubclassOf(it) }) {
                throw e
            }
            Result.failure(e)
        }
    }

    fun <U> invokeOperation(
        function: KCallable<U>,
        args1: Array<*>,
        args2: Array<*>,
        postAssertion: (Result<U>, Result<U>) -> Unit = ::assertEqualsComplete
    ) {
        visited.compute(function) { _, v -> (v ?: 0) + 1 }
        val testRes = runCathing<U> { function.call(test, *args1) }
        val controlRes = runCathing<U> { function.call(control, *args2) }
        postAssertion(testRes, controlRes)
    }
}

fun <U> assertEqualsComplete(testRes: Result<U>, controlRes: Result<U>) {
    assertTrue(testRes.isSuccess == controlRes.isSuccess, "Exactly one failed")
    if (testRes.isSuccess && controlRes.isSuccess) {
        val testVal = testRes.getOrThrow()
        val controlVal = controlRes.getOrThrow()
        if (testVal == null && controlVal == null) {
            Unit
        } else {
            if (testVal!!::class == ByteArray::class)
                assertContentEquals(testVal as ByteArray, controlVal as ByteArray)
            else
                assertEquals(testRes, controlRes)
        }
    }
}


fun <T> FuzzedDataProvider.template(
    source: T,
    buf: T,
    data: FuzzedDataProvider,
    funs: Array<KFunction<*>>,
    genArgsFallback: KCallable<*>.() -> Array<*> = { error("Unexpected method: $this") }
): List<KCallable<*>> {
    val couple = Couple(source, buf)
    val ops = mutableListOf<KCallable<*>>()
    val n = consumeInt(0, Constants.MAX_OPERATIONS_NUMBER)
    repeat(n) {
        val op = pickValue(funs)
        ops += op

        val args = op.generateArguments(data, skipFirst = true, genArgsFallback)
        val args2 = op.copyArguments(args, data)
        couple.invokeOperation(op, args, args2)
    }
    return ops
}
