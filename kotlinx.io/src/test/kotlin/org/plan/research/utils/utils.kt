package org.plan.research.utils

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.io.IOException
import org.plan.research.Constants
import org.plan.research.RealSourceVsBuffer
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        RealSourceVsBuffer.visited.compute(function) { _, v -> (v ?: 0) + 1 }
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