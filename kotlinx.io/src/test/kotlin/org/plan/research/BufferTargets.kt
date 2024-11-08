package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.Buffer
import org.plan.research.utils.ReflectionUtils
import org.plan.research.utils.defaultParams
import org.plan.research.utils.template

object BufferTargets {
    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun randomOps(data: FuzzedDataProvider): Unit = with(data) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)
        val buf = Buffer().apply { write(initBytes) }
        val sec = Buffer().apply { write(initBytes) }
        template(buf, sec, data, ReflectionUtils.bufferFunctions) {defaultParams(data)}
    }
}