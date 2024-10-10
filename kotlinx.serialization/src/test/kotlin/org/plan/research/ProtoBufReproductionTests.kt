package org.plan.research

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

data class Condition<T>(val encodeDefaults: Boolean, val clazz: Class<T>, val bytes: ByteArray)

object ProtoBufReproductionTests {
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `different encodings for empty messages`() {
        val conditions = listOf(
            Condition(false, Int::class.java, byteArrayOf(9)), // -> []
            Condition(false, OneOfType::class.java, byteArrayOf(41)), // -> []
            Condition(false, OneOfType::class.java, byteArrayOf(41, 62)), // -> []
            Condition(false, TestEnum::class.java, byteArrayOf(49)), // -> []
            Condition(false, ProtobufMessageInt::class.java, byteArrayOf(61)), // -> []
            Condition(false, ProtobufMessageInt::class.java, byteArrayOf(77)), // -> [] 
        )
        val messages = List(conditions.size) { i ->
            val serializer = ProtoBuf { encodeDefaults = conditions[i].encodeDefaults }
            when (conditions[i].clazz) {
                Int::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Int>>(conditions[i].bytes)
                Long::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Long>>(conditions[i].bytes)
                Float::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Float>>(conditions[i].bytes)
                Double::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Double>>(conditions[i].bytes)
                Boolean::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Boolean>>(conditions[i].bytes)
                String::class.java -> serializer.decodeFromByteArray<ProtobufMessage<String>>(conditions[i].bytes)
                TestEnum::class.java -> serializer.decodeFromByteArray<ProtobufMessage<TestEnum>>(conditions[i].bytes)
                OneOfType::class.java -> serializer.decodeFromByteArray<ProtobufMessage<OneOfType>>(conditions[i].bytes)
                ListInt::class.java -> serializer.decodeFromByteArray<ProtobufMessage<ListInt>>(conditions[i].bytes)
                MapStringInt::class.java -> serializer.decodeFromByteArray<ProtobufMessage<MapStringInt>>(conditions[i].bytes)
                ProtobufMessageInt::class.java -> serializer.decodeFromByteArray<ProtobufMessage<ProtobufMessageInt>>(conditions[i].bytes)

                else -> error("unreachable")
            }
        }
        val bytesAfterDecodeEncode = List(conditions.size) { i ->
            val serializer = ProtoBuf { encodeDefaults = conditions[i].encodeDefaults }
            when (conditions[i].clazz) {
                Int::class.java -> serializer.encodeToByteArray<ProtobufMessage<Int>>(messages[i] as ProtobufMessage<Int>)
                Long::class.java -> serializer.encodeToByteArray<ProtobufMessage<Long>>(messages[i] as ProtobufMessage<Long>)
                Float::class.java -> serializer.encodeToByteArray<ProtobufMessage<Float>>(messages[i] as ProtobufMessage<Float>)
                Double::class.java -> serializer.encodeToByteArray<ProtobufMessage<Double>>(messages[i] as ProtobufMessage<Double>)
                Boolean::class.java -> serializer.encodeToByteArray<ProtobufMessage<Boolean>>(messages[i] as ProtobufMessage<Boolean>)
                String::class.java -> serializer.encodeToByteArray<ProtobufMessage<String>>(messages[i] as ProtobufMessage<String>)
                TestEnum::class.java -> serializer.encodeToByteArray<ProtobufMessage<TestEnum>>(messages[i] as ProtobufMessage<TestEnum>)
                OneOfType::class.java -> serializer.encodeToByteArray<ProtobufMessage<OneOfType>>(messages[i] as ProtobufMessage<OneOfType>)
                ListInt::class.java -> serializer.encodeToByteArray<ProtobufMessage<ListInt>>(messages[i] as ProtobufMessage<ListInt>)
                MapStringInt::class.java -> serializer.encodeToByteArray<ProtobufMessage<MapStringInt>>(messages[i] as ProtobufMessage<MapStringInt>)
                ProtobufMessageInt::class.java -> serializer.encodeToByteArray<ProtobufMessage<ProtobufMessageInt>>(messages[i] as ProtobufMessage<ProtobufMessageInt>)

                else -> error("unreachable")
            }
        }
        for (conditionsResults in conditions zip bytesAfterDecodeEncode) {
            assertTrue { conditionsResults.first.bytes.contentEquals(conditionsResults.second) }
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

        println(bytesForPrimitiveMessages)
        println(bytesForNonPrimitiveMessages)

        assertTrue { bytesForPrimitiveMessages.size == 1}
        assertTrue { bytesForNonPrimitiveMessages.size == 1}
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
        val messages = List(conditions.size) { i ->
            val serializer = ProtoBuf { encodeDefaults = conditions[i].encodeDefaults }
            when (conditions[i].clazz) {
                Int::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Int>>(conditions[i].bytes)
                Long::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Long>>(conditions[i].bytes)
                Float::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Float>>(conditions[i].bytes)
                Double::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Double>>(conditions[i].bytes)
                Boolean::class.java -> serializer.decodeFromByteArray<ProtobufMessage<Boolean>>(conditions[i].bytes)
                String::class.java -> serializer.decodeFromByteArray<ProtobufMessage<String>>(conditions[i].bytes)
                TestEnum::class.java -> serializer.decodeFromByteArray<ProtobufMessage<TestEnum>>(conditions[i].bytes)
                OneOfType::class.java -> serializer.decodeFromByteArray<ProtobufMessage<OneOfType>>(conditions[i].bytes)
                ListInt::class.java -> serializer.decodeFromByteArray<ProtobufMessage<ListInt>>(conditions[i].bytes)
                MapStringInt::class.java -> serializer.decodeFromByteArray<ProtobufMessage<MapStringInt>>(conditions[i].bytes)
                ProtobufMessageInt::class.java -> serializer.decodeFromByteArray<ProtobufMessage<ProtobufMessageInt>>(conditions[i].bytes)

                else -> error("unreachable")
            }
        }
        val bytesAfterDecodeEncode = List(conditions.size) { i ->
            val serializer = ProtoBuf { encodeDefaults = conditions[i].encodeDefaults }
            when (conditions[i].clazz) {
                Int::class.java -> serializer.encodeToByteArray<ProtobufMessage<Int>>(messages[i] as ProtobufMessage<Int>)
                Long::class.java -> serializer.encodeToByteArray<ProtobufMessage<Long>>(messages[i] as ProtobufMessage<Long>)
                Float::class.java -> serializer.encodeToByteArray<ProtobufMessage<Float>>(messages[i] as ProtobufMessage<Float>)
                Double::class.java -> serializer.encodeToByteArray<ProtobufMessage<Double>>(messages[i] as ProtobufMessage<Double>)
                Boolean::class.java -> serializer.encodeToByteArray<ProtobufMessage<Boolean>>(messages[i] as ProtobufMessage<Boolean>)
                String::class.java -> serializer.encodeToByteArray<ProtobufMessage<String>>(messages[i] as ProtobufMessage<String>)
                TestEnum::class.java -> serializer.encodeToByteArray<ProtobufMessage<TestEnum>>(messages[i] as ProtobufMessage<TestEnum>)
                OneOfType::class.java -> serializer.encodeToByteArray<ProtobufMessage<OneOfType>>(messages[i] as ProtobufMessage<OneOfType>)
                ListInt::class.java -> serializer.encodeToByteArray<ProtobufMessage<ListInt>>(messages[i] as ProtobufMessage<ListInt>)
                MapStringInt::class.java -> serializer.encodeToByteArray<ProtobufMessage<MapStringInt>>(messages[i] as ProtobufMessage<MapStringInt>)
                ProtobufMessageInt::class.java -> serializer.encodeToByteArray<ProtobufMessage<ProtobufMessageInt>>(messages[i] as ProtobufMessage<ProtobufMessageInt>)

                else -> error("unreachable")
            }
        }
        for (conditionsResults in conditions zip bytesAfterDecodeEncode) {
            assertTrue { conditionsResults.first.bytes.contentEquals(conditionsResults.second) }
        }
    }
}