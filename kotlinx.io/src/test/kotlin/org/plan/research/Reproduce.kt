package org.plan.research

import kotlinx.io.Buffer
import kotlinx.io.asInputStream
import kotlinx.io.bytestring.ByteString
import kotlinx.io.indexOf
import kotlinx.io.write
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

object Reproduce {
    @Test
    fun bufferIndexOf() {
        val test = Buffer()
        test.writeByte(0)
        val copy = test.copy()

        assertThrows<Throwable> { test.write(Buffer().asInputStream(), 1) }
        assertThrows<Throwable> { copy.write(Buffer().asInputStream(), 1) }

        val bs = ByteString(0, 0)
        val idx = -1L
        assertEquals(-1, test.indexOf(bs, idx))
        assertEquals(-1, copy.indexOf(bs, idx)) // fails, indexOf == 0
        /*
        inconsistency with Source.indexOf, it fails when startIndex = -1

        otherwise this is probably fine, because `indexOf` doesn't suppose to check buffer boundaries
         */
    }
}
