package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import org.plan.research.utils.ReflectionUtils
import org.plan.research.utils.ReflectionUtils.removeLongOps
import org.plan.research.utils.template

object RealSourceVsBuffer {
    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun randomOps(data: FuzzedDataProvider): Unit = with(data) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)

        val source = initBytes.inputStream().asSource().buffered()
        val buf: Source = Buffer().apply { write(initBytes) }

        template(source, buf, data, ReflectionUtils.sourceFunctions)
    }

    val manyBytes =
        SystemFileSystem.source(Path("data.bin")).buffered().readByteArray()

    val manyBuf = Buffer().apply { write(manyBytes) }
    val fastOps = ReflectionUtils.sourceFunctions
        .removeLongOps()
        .filterNot { it.name == "readAtMostTo" }
        .toTypedArray()

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun randomOpsManyData(data: FuzzedDataProvider): Unit = with(data) {
        val source = manyBytes.inputStream().asSource().buffered()
        val buf: Source = manyBuf.copy()
        template(source, buf, data, fastOps)
    }
}


