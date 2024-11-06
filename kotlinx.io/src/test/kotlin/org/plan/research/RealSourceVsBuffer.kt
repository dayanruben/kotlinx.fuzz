package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.plan.research.utils.ReflectionUtils
import org.plan.research.utils.copyArguments
import org.plan.research.utils.generateArguments
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
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

        val couple = Couple(source, buf)
        val funs: Array<KFunction<*>> = ReflectionUtils.sourceFunctions
        val ops = mutableListOf<KCallable<*>>()
        val n = consumeInt(0, 100)
        repeat(n) {
            val op = pickValue(funs)
            ops += op

            val args = op.generateArguments(data)
            val args2 = op.copyArguments(args, data)
            couple.invokeOperation(op, args, args2)
        }
    }
}

class Couple<T>(val test: T, val control: T) {
    companion object {
        inline fun <R> runCathing(
            catchingClass: KClass<out Throwable> = Exception::class,
            block: () -> R
        ): Result<R> = try {
            Result.success(block())
        } catch (e: Throwable) {
            if (!e::class.isSubclassOf(catchingClass)) throw e
            Result.failure(e)
        }
    }

    fun <U> invokeOperation(
        function: KCallable<U>,
        args1: Array<*>,
        args2: Array<*>,
        postAssertion: (Result<U>, Result<U>) -> Unit = { testRes, controlRes ->
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
    ) {
        val testRes = runCathing<U> { function.call(test, *args1) }
        val controlRes = runCathing<U> { function.call(control, *args2) }
        postAssertion(testRes, controlRes)
    }
}

