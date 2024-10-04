package org.plan.research

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

@OptIn(ExperimentalSerializationApi::class)
object CborReproductionTests {
    @Test
    fun `unhandled illegal state exception`() {
        val byteArray = byteArrayOf(126)
        val serializer = Cbor.Default
        assertThrows<SerializationException> {
            serializer.decodeFromByteArray<String>(byteArray)
        }
    }

    @Test
    fun `unhandled negative array size exception`() {
        val byteArray = byteArrayOf(127, 40)
        val serializer = Cbor.Default
        assertThrows<SerializationException> {
            serializer.decodeFromByteArray<String>(byteArray)
        }
    }

    @Test
    fun `unhandled stack overflow error`() {
        val byteArray = byteArrayOf(127, 0, 0)
        val serializer = Cbor.Default
        assertThrows<SerializationException> {
            serializer.decodeFromByteArray<String>(byteArray)
        }
    }
}