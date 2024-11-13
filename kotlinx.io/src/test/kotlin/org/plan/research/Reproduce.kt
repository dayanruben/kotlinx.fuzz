package org.plan.research

import kotlinx.io.Buffer
import kotlinx.io.readAtMostTo
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.test.Test

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
}

class RandomInputStream(val random: Random) : InputStream() {
    override fun read(): Int {
        return random.nextInt(0, 255)
    }
}