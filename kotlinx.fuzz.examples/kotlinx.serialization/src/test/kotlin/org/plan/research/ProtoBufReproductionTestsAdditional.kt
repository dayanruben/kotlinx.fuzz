@file:Suppress("UNCHECKED_CAST")

package org.plan.research

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertTrue

// Conditions for reproduction
data class Condition<T>(val encodeDefaults: Boolean, val messageType: Class<T>, val bytes: ByteArray)

@OptIn(ExperimentalSerializationApi::class)
fun <T> deserializePolymorphicMessage(condition: Condition<T>): ProtobufMessage<T> {
    val serializer = ProtoBuf { encodeDefaults = condition.encodeDefaults }
    return when (condition.messageType) {
        Int::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Int>>(condition.bytes) as ProtobufMessage<T>
        Long::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Long>>(condition.bytes) as ProtobufMessage<T>
        Float::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Float>>(condition.bytes) as ProtobufMessage<T>
        Double::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Double>>(condition.bytes) as ProtobufMessage<T>
        Boolean::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Boolean>>(condition.bytes) as ProtobufMessage<T>
        String::class.java -> serializer.decodeFromByteArray<ProtobufMessage<String>>(condition.bytes) as ProtobufMessage<T>
        TestEnum::class.java -> serializer.decodeFromByteArray<ProtobufMessage<TestEnum>>(condition.bytes) as ProtobufMessage<T>
        OneOfType::class.java -> serializer.decodeFromByteArray<ProtobufMessage<OneOfType>>(condition.bytes) as ProtobufMessage<T>
        ListInt::class.java -> serializer.decodeFromByteArray<ProtobufMessage<ListInt>>(condition.bytes) as ProtobufMessage<T>
        MapStringInt::class.java -> serializer.decodeFromByteArray<ProtobufMessage<MapStringInt>>(condition.bytes) as ProtobufMessage<T>
        ProtobufMessageInt::class.java -> serializer.decodeFromByteArray<ProtobufMessage<ProtobufMessageInt>>(condition.bytes) as ProtobufMessage<T>

        else -> error("unreachable")
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> serializePolymorphicMessage(condition: Condition<T>, message: ProtobufMessage<*>): ByteArray {
    val serializer = ProtoBuf { encodeDefaults = condition.encodeDefaults }
    return when (condition.messageType) {
        Int::class.java -> serializer.encodeToByteArray<ProtobufMessage<Int>>(message as ProtobufMessage<Int>)
        Long::class.java -> serializer.encodeToByteArray<ProtobufMessage<Long>>(message as ProtobufMessage<Long>)
        Float::class.java -> serializer.encodeToByteArray<ProtobufMessage<Float>>(message as ProtobufMessage<Float>)
        Double::class.java -> serializer.encodeToByteArray<ProtobufMessage<Double>>(message as ProtobufMessage<Double>)
        Boolean::class.java -> serializer.encodeToByteArray<ProtobufMessage<Boolean>>(message as ProtobufMessage<Boolean>)
        String::class.java -> serializer.encodeToByteArray<ProtobufMessage<String>>(message as ProtobufMessage<String>)
        TestEnum::class.java -> serializer.encodeToByteArray<ProtobufMessage<TestEnum>>(message as ProtobufMessage<TestEnum>)
        OneOfType::class.java -> serializer.encodeToByteArray<ProtobufMessage<OneOfType>>(message as ProtobufMessage<OneOfType>)
        ListInt::class.java -> serializer.encodeToByteArray<ProtobufMessage<ListInt>>(message as ProtobufMessage<ListInt>)
        MapStringInt::class.java -> serializer.encodeToByteArray<ProtobufMessage<MapStringInt>>(message as ProtobufMessage<MapStringInt>)
        ProtobufMessageInt::class.java -> serializer.encodeToByteArray<ProtobufMessage<ProtobufMessageInt>>(message as ProtobufMessage<ProtobufMessageInt>)

        else -> error("unreachable")
    }
}

object ProtoBufReproductionTestsAdditional {
    @Test
    fun `different encodings for empty messages`() {
        val conditions = listOf(
            Condition(false, Int::class.java, byteArrayOf(9)),
            Condition(false, OneOfType::class.java, byteArrayOf(41)),
            Condition(false, OneOfType::class.java, byteArrayOf(41, 62)),
            Condition(false, TestEnum::class.java, byteArrayOf(49)),
            Condition(false, ProtobufMessageInt::class.java, byteArrayOf(61)),
            Condition(false, ProtobufMessageInt::class.java, byteArrayOf(77)),
        )

        for (condition in conditions) {
            val message = deserializePolymorphicMessage(condition)
            // We expect that decode-encode sequence is identity transformation, but it isn't
            assertTrue { condition.bytes contentEquals serializePolymorphicMessage(condition, message) }
        }
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
        val messageMap = messageInt as ProtobufMessage<MapStringInt>
        val messageDouble = messageInt as ProtobufMessage<Double>
        val messageLong = messageInt as ProtobufMessage<Long>
        val messageBoolean = messageInt as ProtobufMessage<Boolean>

        val serializer = ProtoBuf { encodeDefaults = true }
        val bytesForPrimitiveMessages = setOf(
            serializer.encodeToHexString<ProtobufMessage<Int>>(messageInt),
            serializer.encodeToHexString<ProtobufMessage<Long>>(messageLong),
            serializer.encodeToHexString<ProtobufMessage<Boolean>>(messageBoolean),
            serializer.encodeToHexString<ProtobufMessage<Double>>(messageDouble)
        )
        val bytesForNonPrimitiveMessages = setOf(
            serializer.encodeToHexString<ProtobufMessage<String>>(messageString),
            serializer.encodeToHexString<ProtobufMessage<MapStringInt>>(messageMap)
        )

        assertTrue { bytesForPrimitiveMessages.size == 1}
        assertTrue { bytesForNonPrimitiveMessages.size == 1}
        assertTrue { serializer.decodeFromHexString<ProtobufMessage<Int>>(bytesForPrimitiveMessages.first()) ==
                     serializer.decodeFromHexString<ProtobufMessage<String>>(bytesForNonPrimitiveMessages.first()) as ProtobufMessage<Int>}
        // We expect identical messages to be encoded equally.
        // We know that they are identical since assertion on line 112 is passed.
        // Moreover, we know that for all primitive types byte arrays are equal since assertion on line 110 passed.
        // The same works for non-primitive types and assertion on line 111
        assertTrue { bytesForPrimitiveMessages.first() == bytesForNonPrimitiveMessages.first() }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `different encoding for not empty messages`() {
        val conditions = listOf(
            Condition(false, Int::class.java, byteArrayOf(
                -22, 52, -23, 10, 10, 10, 10, 10, 10, 10, 10, 10, -3, 24, 10, 10, 10, 10, 10, -87, 10, 10, 10, 10, 10, 10, 10,
                10, 10, 10, 10, -118, 10, 0, 10, 10, 10, 10, 10, -30, 125, 0, -30, 125, 0, 45, 125, -96, -96, 125, -30, 125, 44,
                -30, 125, 0, -30, 125, 0, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, -96, 125, 0, -30, 125, 0, 45, 125,
                -96, 93, 32, -30, 125, 0, 45, 125, -96, -125, -33, 29, 88, 125, 0, 37, 37, 37, 45, -30, 125, 0, -32, -93, 2, 3,
                -32, -93, 2, 3, -32, -93, 2, 3, -32, -93, 2, 3, -32, -93, 2, 3, -32, -93, 2, 3, 125, -32, -93, 2, 3, -32, -93,
                2, 3, 125, 0, -30, 125, 0, 45, 125, -96, -96, 125, -30, 125, 44, -30, 125, 0, -30, 125, 0, 45, 125, -96, -21,
                32, -30, 125, 0, -30, 125, 0, -96, 125, 0, -30, 125, 0, 45, 125, -96, 93, 32, -30, 125, 0, 45, 125, -96, 125,
                32, -30, -96, 125, 0, 37, 37, 37, 45, -30, 125, 0, -32, -93, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 15,
                0, 0, 0, -30, 125, 0, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, 45, 125, -96, -96, 125, 0, -30, 125, 0,
                45, 125, 29, 9, -43, 125, 44, 21, 21, 21, 32, 125, 25, -96, 125, -30, 0, 29, 9, 125, 44, -30, 125, 0, 45, 125,
                -96, 125, 44, -30, 125, 0, -30, 125, 0, 45, 125, -96, 32, 86, 65, 82, 73, 78, 84, 40, 48, 41, 0, -30, 125, 0,
                45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, 45, 125, -96, -96, 125, -30, 125, 44, -30, 125, 0, -30, 125, 0,
                45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, -96, 125, 0, -30, 125, 0, 45, 125, -96, 93, 32, -30, 125, 0,
                45, 125, -96, 125, 32, -30, -96, 125, 0, 37, 37, 37, 45, 125, -96, 125, 44, -30, 125, 0, -30, 0, 45, 125, -96,
                125, 32, -30, 125, 0, -30, 125, 0, -96, 125, -82, 0, -30, 125, 0, 45, 125, -96, 125, 32, -30, 125, 0, 45, 125,
                -96, 125, 44, -30, 125, 0, -30, 125, 0, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, -96, 125, 0, -30, 125,
                0, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, 45, 125, -96, -96, 32, -30, 125, 0, -30, 125, 0, -96, 125,
                0, -30, 125, 0, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, 45, 125, -96, -59, 125, -30, 125, 44, -30, 125,
                0, -30, 125, 0, 45, 125, -96, 125, 32, -30, -96, 125, 0, 37, 37, 37, 41, 125, -96, 125, 2, 3, -32, 32, -30, 44,
                -30, 125, 0, -30, 32, -30, 125, 0, 45, 125, -96, 125, 32, -30, -96, 125, 0, 37, 37, 37, 45, -30, 125, 0, 45, 93,
                -96, -96, 125, -30, 125, 44, -30, 125, 0, -30, 125, 109, 44, -30, 125, 0, -30, 125, 0, 45, 125, 29, 9, -43, 125,
                44, 21, 21, 21, 32, 125, 125, -96, 125, 44, -30, 125, 0, -30, 125, 0, 45, 125, -96, 32, 86, 65, 82, 73, 78, 84,
                40, 48, 41, 0, -30, 125, 0, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, 45, 125, -96, -96, 125, -30, 125,
                44, -30, 125, 0, -30, 125, 0, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, -96, 125, 0, -30, 125, 0, 45,
                125, -96, 93, 32, -30, 125, 0, 45, 125, -96, 125, 32, -30, -96, 125, 0, 37, 37, 37, 45, 125, -96, 125, 44, -30,
                125, 0, -30, 125, 0, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, -96, 125, -82, 0, -30, 125, 0, 45, 125,
                -96, 125, 32, -30, 125, 0, 45, 125, -96, 125, 44, -30, 125, 0, -30, 125, 0, 45, 125, -96, 125, 32, -30, 125, 0,
                -30, 125, 0, -96, 125, 0, -30, 125, 0, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, 45, 125, -96, -96, 32,
                -30, 125, 0, -30, 125, 0, -96, 125, 0, -30, 125, 0, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, 45, 125,
                -96, -59, 125, 49, 44, -30, 125, 0, -30, -81, -1, -1, -1, -1, -1, -1, -1, 125, 0, -30, 125, 0, -96, 125, 0, -30,
                125, 0, 45, 125, -96, 93, 32, -30, 125, 0, 45, 125, -96, 125, 32, -30, -96, 125, 0, 37, 37, 37, 45, 125, -96,
                125, 44, -30, 125, 0, -30, 124, -10, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, -96, 125, -82, 0, -30,
                125, 0, 45, 125, -96, 125, 32, -30, 125, 0, 45, 125, -96, 125, 44, 69, 120, 112, 101, 99, 116, 101, 100, 32,
                119, 105, 114, 110, 100, 32, 105, 54, 52, 40, 49, 41, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, -96, 125, 0, -30, 125, 0, 45, 125, -96,
                125, 32, -30, 125, 0, -30, 125, 0, 45, 125, -96, -96, 125, 33, -126, 44, -30, 125, 0, -30, 125, 0, 45, 125, 9,
                0, 4, 10, 44, -30, 125, 0, -30, 125, 2, 45, 125, 29, 9, -43, 125, 44, 21, 21, 21, 32, 125, 25, -96, 125, -30, 0,
                29, 9, 125, 44, -30, 125, 0, 45, 125, -96, 125, 44, -30, 125, 0, -30, 125, 0, 45, 0, -96, 125, 0, -30, 125, 0,
                45, 125, -30, 125, 44, -30, 125, 0, -30, 125, 0, 45, 125, -96, -21, 32, -30, 125, 0, -30, 125, 0, -96, 125, 0,
                -30, 125, 0, 45, 125, -96, 93, 32, -30, 125, 0, 45, 125, -96, 125, 32, -30, -96, 125, 0, 37, 37, 37, 45, -30,
                125, 0, -32, -93, 2, 3, -32, -93, 2, 3, -32, -93, 2, 3, -32, -93, 2, 3, -32, -93, 2, 3, -32, -93, 2, 3, 125,
                -32, -93, 2, 3, -32, -93, 2, 3, 125, 0, -30, 125, 125, -96, 125, 44, -30, 125, 0, -30, 125, 0, 45, 125, -96,
                125, 32, -30, 125, 0, -30, 125, 0, -96, 125, 0, -30, 125, 0, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0,
                45, 125, -96, -96, 125, -30, 125, 44, -30, 125, 0, -30, 9, 10, 125, 0, 45, 125, -96, 125, 32, -30, 125, 0, -30,
                125, 0, -96, 125, 0, -30, 125, 0, 45, 125, -96, 93, 32, -30, 125, 0, 45, 125, -96, 125, 32, -30, -96, 125, 0,
                37, 37, 37, 45, -30, 125, 0, -32, -93, 2, 3, -32, -93, 2, 3, 125, -30, 125, 44, -30, 125, 0, -30, 125, 2, 45,
                125, 29, 9, -43, 125, 44, 21, 21, 21, 32, 125, 25, -96, 0, 45, 125, -96, 125, 32, -30, 10, 10, 10, 10, 10, 10,
                10, 10, 10, -3, 24, 10, 10, 10, 10, 10, -87, 10, 10, 14, 10, 10, 10, 10, 125, 0, -30, 125, 0, 45, 125, -96, -96,
                125, -30, 125, 44, -30, 125, 0, -30, 125, 0, 45, 125, -96, 125, 32, -30, 125, 0, -30, 125, 0, -96, 125, 0, -30,
                125, 0, 45, 125, -96, 93, 32, -30, 125, 0, 45, 125, -96, -125, -33, 29, -93, 2, 3, 125, 0, -30, 125, 0, 45, 125,
                -96, -96, 125, -30, 125, 44, -30, 125, 0, 63, -30, 125, 0, 45, 125, -96, -21, 32, 125, 0, -30, 125, 0, -96, 125,
                37, 44, -30, 125, 0, 37, 37, 37, 45, 0, 0)),
            Condition(true, ProtobufMessageInt::class.java, byteArrayOf(-30, 125, 0, 125)), // -> [-24, 23, 5, -30, 125, 3, -24, 23, 5]
            Condition(true, Long::class.java, byteArrayOf(-55, 40)), // -> [-24, 23, 5, -55, 40, -1, -1, -1, -1, -1, -1, -1, -1, -70, -108, 4, 0]
        )

        for (condition in conditions) {
            val message = deserializePolymorphicMessage(condition)
            // We expect that decode-encode sequence is identity transformation, but it isn't
            assertTrue { condition.bytes contentEquals serializePolymorphicMessage(condition, message) }
        }
    }
}