package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.Buffer
import kotlinx.io.write
import org.plan.research.utils.callOps
import org.plan.research.utils.defaultParams
import java.io.InputStream
import kotlin.reflect.KFunction

object BuffersJvmKt {
    val w2: Buffer.(InputStream, Long) -> Buffer = Buffer::write

    val funs: Array<KFunction<*>> =
        arrayOf(Buffer::copyTo as KFunction<*>, Buffer::readTo as KFunction<*>, w2 as KFunction<*>)

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun randomOps(data: FuzzedDataProvider): Unit = with(data) {
        val initBytes = consumeBytes(Constants.INIT_BYTES_COUNT)
        val buf = Buffer().apply { write(initBytes) }
        val ops = consumeInt(0, Constants.MAX_OPERATIONS_NUMBER)
        buf.callOps(ops, funs, data) { defaultParams(data) }
    }
}