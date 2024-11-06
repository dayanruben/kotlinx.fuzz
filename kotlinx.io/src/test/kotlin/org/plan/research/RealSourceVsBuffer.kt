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
import kotlin.reflect.KFunction

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