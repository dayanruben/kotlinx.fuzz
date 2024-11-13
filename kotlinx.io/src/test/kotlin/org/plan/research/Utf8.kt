package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.Buffer
import kotlinx.io.readString
import kotlinx.io.writeString
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
}