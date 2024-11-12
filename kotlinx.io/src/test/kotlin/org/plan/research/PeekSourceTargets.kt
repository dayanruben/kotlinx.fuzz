package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.plan.research.utils.Couple
import org.plan.research.utils.ReflectionUtils
import org.plan.research.utils.ReflectionUtils.removeLongOps
import org.plan.research.utils.copyArguments
import org.plan.research.utils.generateArguments
import kotlin.reflect.KCallable

object PeekSourceTargets {
    @FuzzTest
    fun randomOps(data: FuzzedDataProvider): Unit = with(data) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)
        val fromRealSource = initBytes.inputStream().asSource().buffered().peek()
        val fromBuffer = Buffer().apply { write(initBytes) }.peek()

        val couple = Couple<Source>(fromRealSource, fromBuffer)

        val ops1 = doSomeOps(data, couple)
        val peekCouple = Couple<Source>(fromRealSource.peek(), fromBuffer.peek())
        val ops2 = doSomeOps(data, peekCouple)
        val ops3 = doSomeOps(data, couple)
    }

    val fastOps = ReflectionUtils.sourceFunctions.removeLongOps()


    private fun FuzzedDataProvider.doSomeOps(
        data: FuzzedDataProvider,
        couple: Couple<Source>
    ): List<KCallable<*>> {
        val n = consumeInt(0, Constants.MAX_OPERATIONS_NUMBER / 3)
        val ops = mutableListOf<KCallable<*>>()
        repeat(n) {
            val op = pickValue(fastOps)
            ops += op

            val args = op.generateArguments(data, skipFirst = true)
            val args2 = op.copyArguments(args, data)
            couple.invokeOperation<Any?>(op, args, args2)
        }
        return ops
    }
}