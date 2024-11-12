package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.Buffer
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import org.plan.research.utils.ReflectionUtils
import org.plan.research.utils.defaultParams
import org.plan.research.utils.template
import java.io.ByteArrayOutputStream
import kotlin.test.assertContentEquals

object SinkTests {
    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun aa(data: FuzzedDataProvider): Unit = with(data) {
        val ostream = ByteArrayOutputStream()
        val sink = ostream.asSink().buffered()
        val buf = Buffer()

        val ops = template(sink, buf, data, ReflectionUtils.sinkFunctions) { defaultParams(data) }
        sink.flush()
        val sinkRes = ostream.toByteArray()
        val bufRes = buf.readByteArray()
        assertContentEquals(sinkRes, bufRes)
    }
}

