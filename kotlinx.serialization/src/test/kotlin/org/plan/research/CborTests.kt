package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlin.test.assertEquals

object CborTests {
    private fun isCborDecodingException(e: Throwable): Boolean =
        e.javaClass.name == "kotlinx.serialization.cbor.internal.CborDecodingException"
    private fun isSerializerSubclassException(e: Throwable): Boolean =
        e.javaClass.name == "kotlinx.serialization.SerializationException"
                && e.message.orEmpty().startsWith("Serializer for subclass")

    @OptIn(ExperimentalSerializationApi::class)
    private fun FuzzedDataProvider.cborSerializer(): Cbor = Cbor {
        encodeDefaults = consumeBoolean()
        ignoreUnknownKeys = consumeBoolean()
        encodeKeyTags = consumeBoolean()
        encodeValueTags = consumeBoolean()
        encodeObjectTags = consumeBoolean()
        verifyKeyTags = consumeBoolean()
        verifyValueTags = consumeBoolean()
        verifyObjectTags = consumeBoolean()
        useDefiniteLengthEncoding = consumeBoolean()
        preferCborLabelsOverNames = consumeBoolean()
        alwaysUseByteString = consumeBoolean()
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = TEST_DURATION)
    fun cborParseByteArray(dataProvider: FuzzedDataProvider) {
        val cbor = dataProvider.cborSerializer()
        val parseTarget = dataProvider.consumeInt(0, 10)
        val byteArray = dataProvider.consumeRemainingAsBytes()
        try {
            val res: Any? = when (parseTarget) {
                0 -> cbor.decodeFromByteArray<String>(byteArray)
                1 -> cbor.decodeFromByteArray<Int>(byteArray)
                2 -> cbor.decodeFromByteArray<Long>(byteArray)
                3 -> cbor.decodeFromByteArray<Float>(byteArray)
                4 -> cbor.decodeFromByteArray<Double>(byteArray)
                5 -> cbor.decodeFromByteArray<Value>(byteArray)
                6 -> cbor.decodeFromByteArray<List<Value>>(byteArray)
                7 -> cbor.decodeFromByteArray<Map<String, Value>>(byteArray)
                9 -> cbor.decodeFromByteArray<List<List<Value>>>(byteArray)
                10 -> cbor.decodeFromByteArray<List<Map<String, Value>>>(byteArray)
                else -> null
            }
            println(res)
        } catch (e: NegativeArraySizeException) {
            // probably a bug?
            // not interesting??
            return
        } catch (e: IllegalStateException) {
            // not interesting??
            return
        } catch (e: StackOverflowError) {
            // probably a bug?
            // not interesting??
            return
        } catch (e: Throwable) {
            if (isCborDecodingException(e)) {
                return
            } else if (isSerializerSubclassException(e)) {
                return
            } else {
                throw e
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = TEST_DURATION)
    fun cborEncodeString(dataProvider: FuzzedDataProvider) {
        val cbor = dataProvider.cborSerializer()
        val str = dataProvider.consumeRemainingAsString()
        try {
            cbor.encodeToByteArray<String>(str)
        } catch (e: Throwable) {
            if (isCborDecodingException(e)) {
                return
            } else {
                throw e
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = TEST_DURATION)
    fun cborEncodeAndDecode(dataProvider: FuzzedDataProvider) {
        try {
            val cbor = dataProvider.cborSerializer()
            val value = dataProvider.generateValue()
            val json = cbor.encodeToByteArray<Value>(value)
            val decoded = cbor.decodeFromByteArray<Value>(json)
            assertEquals(value, decoded)
        } catch (e: Throwable) {
            throw e
        }
    }
}
