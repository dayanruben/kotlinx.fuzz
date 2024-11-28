package org.plan.research

import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

object Reproduce {
    @Test
    fun a() {
        val initBytes = byteArrayOf(109, 84, 104, 46, 44, 73, -29, 103, 117, -1)
        val buf = Buffer().apply { write(initBytes) }
        println(buf.readByte())
        println()
        val copy = buf.copy()
        buf.writeShort(228)
        copy.writeShort(228)
        val bf1 = ByteBuffer.allocate(10)
        val bf2 = ByteBuffer.allocate(10)
        println(buf.readAtMostTo(bf1))
        val message = copy.readAtMostTo(bf2)
        println(message)
//        println(buf.readString() == copy.readString())
//        println(buf.exhausted())
//        println(copy.exhausted())
    }

    @Test
    fun writeData() {
        val random = Random(777)
        val f = File("data.bin")
        f.writeBytes(random.nextBytes(2L.shl(16).toInt()))
    }

    @Test
    fun bufferCopyTest() {
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
        this is probably fine, because `indexOf` doesn't suppose to check buffer boundaries
         */
    }
}
