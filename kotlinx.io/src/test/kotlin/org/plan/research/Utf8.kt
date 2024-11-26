package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.*
import kotlin.test.assertEquals

object Utf8 {
    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun bufferWriteReadString(data: FuzzedDataProvider): Unit = with(data) {
        val s = data.consumeRemainingAsString()
        val buf = Buffer()
        buf.writeString(s)
        val r = buf.readString()
        assertEquals(s, r)
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun commonReadUtf8CodePoint(data: FuzzedDataProvider): Unit = with(data) {
        val buf = Buffer()
        val codePoint = data.consumeCharNoSurrogates()
        val length = codePoint.toString().encodeToByteArray().size.toLong()
        buf.writeCodePointValue(codePoint.code)
        val size = buf.size
        assertEquals(length, size)
        val cp = if (data.consumeBoolean()){
            (buf as Source).readCodePointValue()
        } else {
            (buf as Source).buffered().readCodePointValue()
        }
        assertEquals(cp.toChar(), codePoint)
    }
}