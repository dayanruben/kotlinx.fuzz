package org.plan.research

import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

object Reproduce {
    @Test
    fun `Bufer$indexOf and Source$indexOf inconsistency`() {
        val buf = Buffer()
        val bs = ByteString(0, 0)
        val idx = -1L
        assertEquals(-1, buf.indexOf(bs, idx))
        assertEquals(-1, (buf as Source).indexOf(bs, idx))
        //  inconsistency with Source.indexOf, it fails when startIndex = -1
    }

    @Test
    fun `Bufer$indexOf and Buffer$copy works strangely`() {
        val origBuf = Buffer()
        origBuf.writeByte(0)
        val copyBuf = origBuf.copy()

        assertThrows<Throwable> { origBuf.write(Buffer().asInputStream(), 1) }
        assertThrows<Throwable> { copyBuf.write(Buffer().asInputStream(), 1) }

        val bs = ByteString(0, 0)
        val idx = -1L
        assertEquals(-1, origBuf.indexOf(bs, idx))
        assertEquals(-1, copyBuf.indexOf(bs, idx)) // fails, indexOf == 0
    }
}
