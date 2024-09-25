package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlin.test.assertEquals

object CborTests {
    private const val MAX_STR_LENGTH = 100

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = "1h")
    fun cborParseByteArray(dataProvider: FuzzedDataProvider) {
        val cbor = Cbor.Default
        val byteArray = dataProvider.consumeRemainingAsBytes()
        try {
            cbor.decodeFromByteArray<String>(byteArray)
        } catch (e: NegativeArraySizeException) {
            // probably a bug?
            // not interesting??
        } catch (e: IllegalStateException) {
            // not interesting??
        } catch (e: StackOverflowError) {
            // probably a bug?
            // not interesting??
        } catch (e: SerializationException) {
            if (e.javaClass.name != "kotlinx.serialization.cbor.internal.CborDecodingException") {
                System.err.println("[${byteArray.joinToString(",")}]")
                throw e
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = "1h")
    fun cborEncodeString(dataProvider: FuzzedDataProvider) {
        val cbor = Cbor.Default
        val str = dataProvider.consumeRemainingAsString()
        try {
            cbor.encodeToByteArray<String>(str)
        } catch (e: NegativeArraySizeException) {
            // probably a bug?
            // not interesting??
        } catch (e: IllegalStateException) {
            // not interesting??
        } catch (e: StackOverflowError) {
            // probably a bug?
            // not interesting??
        } catch (e: SerializationException) {
            if (e.javaClass.name != "kotlinx.serialization.cbor.internal.CborDecodingException") {
                System.err.println("\"$str\"")
                throw e
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = "1h")
    fun cborEncodeAndDecode(data: FuzzedDataProvider) {
        val cborer = Cbor {}
        val value = data.generateValue(MAX_STR_LENGTH)
        val json = cborer.encodeToByteArray<Value>(value)
        val decoded = cborer.decodeFromByteArray<Value>(json)
        assertEquals(value, decoded)
    }
}
