@file:Suppress("UNCHECKED_CAST")

package org.plan.research

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertTrue

object ProtoBufReproductionTests {
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `different encodings for empty messages`() {
        val bytes = byteArrayOf(9)
        val message = ProtoBuf.decodeFromByteArray<ProtobufMessage<Int>>(bytes)
        // We expect that decode-encode sequence is identity transformation, but it isn't: [9] != []
        assertTrue { bytes.contentEquals(ProtoBuf.encodeToByteArray(message)) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `different encodings for messages based on primitive and not primitive types`() {
        val messageInt = ProtobufMessage<Int>(
            intFieldDefault = null,
            intFieldFixed = null,
            intFieldSigned = null,
            // longField is 5 by default
            floatField = null,
            doubleField = null,
            stringField = null,
            booleanField = null,
            enumField = null,
            nestedMessageField = null,
            oneOfField = null,
            listField = emptyList(),
            packedListField = emptyList(),
            mapField = emptyMap(),
            packedMapField = emptyMap(),
        )
        val messageString = messageInt as ProtobufMessage<String>

        val serializer = ProtoBuf { encodeDefaults = true }
        val bytesForPrimitiveMessages = serializer.encodeToByteArray<ProtobufMessage<Int>>(messageInt)
        val bytesForNonPrimitiveMessages = serializer.encodeToByteArray<ProtobufMessage<String>>(messageString)

        assertTrue { serializer.decodeFromByteArray<ProtobufMessage<Int>>(bytesForPrimitiveMessages) ==
                     serializer.decodeFromByteArray<ProtobufMessage<Int>>(bytesForNonPrimitiveMessages)}
        // We expect identical messages to be encoded equally.
        // We know that they are identical since assertion on line 45 is passed
        assertTrue { bytesForPrimitiveMessages contentEquals bytesForNonPrimitiveMessages }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `different encoding for not empty messages`() {
        val bytes = byteArrayOf(-30, 125, 0, 125)
        val serializer = ProtoBuf { encodeDefaults = true }
        val message = serializer.decodeFromByteArray<ProtobufMessage<ProtobufMessageInt>>(bytes)

        assertTrue {message == serializer.decodeFromByteArray<ProtobufMessage<ProtobufMessageInt>>(
            serializer.encodeToByteArray(message)
        )}
        // We expect that decode-encode sequence is identity transformation, but it isn't: [-30, 125, 0, 12] != [-24, 23, 5, -30, 125, 3, -24, 23, 5]
        assertTrue { bytes.contentEquals(serializer.encodeToByteArray(message)) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `null values are unsupported for fields with default values`() {
        val message = ProtobufMessage<Int>(
            intFieldDefault = null,
            intFieldFixed = null,
            intFieldSigned = null,
            longField = null, // longField is 5 by default
            floatField = null,
            doubleField = null,
            stringField = null,
            booleanField = null,
            enumField = null,
            nestedMessageField = null,
            oneOfField = null,
            listField = emptyList(),
            packedListField = emptyList(),
            mapField = emptyMap(),
            packedMapField = emptyMap(),
        )

        // Field with default values are prohibited from having null values
        assertDoesNotThrow { ProtoBuf.encodeToByteArray<ProtobufMessage<Int>>(message) }
    }
}