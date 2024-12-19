package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        val originalCodePoint = data.consumeInt()

        try {
            buf.writeCodePointValue(originalCodePoint)
        } catch (e: IllegalArgumentException) {
            if (e.message?.contains("Code point value is out of Unicode codespace") != true) throw e
            return
        }

        val readCodePoint = if (data.consumeBoolean()) {
            buf.readCodePointValue()
        } else {
            (buf as Source).buffered().readCodePointValue()
        }
        when {
            originalCodePoint == 63 -> assertEquals(63, readCodePoint)
            readCodePoint == 63 -> assertTrue { Char(originalCodePoint).isSurrogate() }
            else -> assertEquals(originalCodePoint, readCodePoint)
        }
    }
}