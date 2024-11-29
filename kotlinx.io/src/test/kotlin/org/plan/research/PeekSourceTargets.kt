package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.plan.research.utils.*
import org.plan.research.utils.ReflectionUtils.removeLongOps
import kotlin.reflect.KFunction

object PeekSourceTargets {
    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun randomOps(data: FuzzedDataProvider): Unit = with(data) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)
        val fromRealSource = initBytes.inputStream().asSource().buffered().peek()
        val fromBuffer = Buffer().apply { write(initBytes) }.peek()

        val couple = Couple<Source>(fromRealSource, fromBuffer)

        fun getN(): Int = consumeInt(0, Constants.MAX_OPERATIONS_NUMBER / 3)

        val ops1 = couple.callNOps(getN(), fastOps, data)
        val peekCouple = Couple<Source>(fromRealSource.peek(), fromBuffer.peek())
        val ops2 = peekCouple.callNOps(getN(), fastOps, data)
        val ops3 = couple.callNOps(getN(), fastOps, data)
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun peekWorksSame(data: FuzzedDataProvider): Unit = with(data) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)
        val fromRealSource = initBytes.inputStream().asSource().buffered()

        repeat(getN()) {
            val op = pickValue(fastOps)
            val args = op.generateArguments(data)
            catching { op.call(fromRealSource, *args) }
        }

        val ops = mutableListOf<KFunction<*>>()
        val argCopies = mutableListOf<Array<*>>()
        val ans = mutableListOf<Result<*>>()
        val peekSource = fromRealSource.peek()
        val n = getN()
        repeat(n) {
            val op = pickValue(fastOps)
            ops += op
            val args = op.generateArguments(data)
            argCopies += copyArguments(args, data)
            ans += catching { op.call(peekSource, *args) }
        }

        for (i in 0 until n) {
            val op = ops[i]
            val args = argCopies[i]
            val res = catching { op.call(fromRealSource, *args) }
            assertEqualsComplete(res, ans[i])
        }
    }

    private fun FuzzedDataProvider.getN(): Int = consumeInt(0, Constants.MAX_OPERATIONS_NUMBER / 3)

    private val fastOps = ReflectionUtils.sourceFunctions.removeLongOps()
}