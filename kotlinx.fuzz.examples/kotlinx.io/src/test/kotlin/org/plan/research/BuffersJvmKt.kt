package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.*
import org.plan.research.utils.callOps
import org.plan.research.utils.defaultParams
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import kotlin.reflect.KFunction

object BuffersJvmKt {
    private val WRITE: Buffer.(InputStream, Long) -> Buffer = Buffer::write
    private val COPY_TO: Buffer.(OutputStream, Long, Long) -> Unit = Buffer::copyTo
    private val READ_TO: Buffer.(OutputStream, Long) -> Unit = Buffer::readTo
    private val TRANSFER_FROMIS: Buffer.(InputStream) -> Buffer = Buffer::transferFrom
    private val TRANSFER_FROMBB: Buffer.(ByteBuffer) -> Buffer = Buffer::transferFrom
    private val READ_AT_MOST_TO: Buffer.(ByteBuffer) -> Int = Buffer::readAtMostTo

    val funs: Array<KFunction<*>> = arrayOf(
        WRITE, COPY_TO, READ_TO, TRANSFER_FROMBB, TRANSFER_FROMIS, READ_AT_MOST_TO
    ).map { it as KFunction<*> }.toTypedArray()

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun randomOps(data: FuzzedDataProvider): Unit = with(data) {
        val initBytes = consumeBytes(Constants.INIT_BYTES_COUNT)
        val buf = Buffer().apply { write(initBytes) }
        val ops = consumeInt(0, Constants.MAX_OPERATIONS_NUMBER)
        buf.callOps(ops, funs, data) { defaultParams(data) }
    }
}