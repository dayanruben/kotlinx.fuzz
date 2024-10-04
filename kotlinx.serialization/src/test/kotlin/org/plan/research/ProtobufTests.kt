package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.serialization.protobuf.*
import kotlinx.serialization.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.lang.IndexOutOfBoundsException

fun ByteArray.toAsciiHexString() = joinToString("") {
    "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

private fun checkCauses(e: Throwable, pred: (String?) -> Boolean): Boolean {
    if (pred(e.message)) return true
    if (e.cause == null) return false
    return checkCauses(e.cause!!, pred)
}

object ProtobufTests {
    private const val MAX_STR_LENGTH = 1000
    private const val MAX_DURATION = "20m"

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = MAX_DURATION)
    fun protoBufEncodeToByteArray(data: FuzzedDataProvider) {
        val serializer = ProtoBuf { encodeDefaults = data.consumeBoolean() }
        val message = data.generateValue(MAX_STR_LENGTH)
        try {
            serializer.encodeToByteArray(message)
        } catch (e: Exception) {
            System.err.println("[${message}]")
            throw e
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = MAX_DURATION)
    fun protoBufDecodeFromByteArray(data: FuzzedDataProvider) {
        val serializer = ProtoBuf { encodeDefaults = data.consumeBoolean() }
        val bytes = data.consumeRemainingAsBytes()
        if (bytes.isEmpty()) return
        try {
            val message = serializer.decodeFromByteArray<Value>(bytes)
            assertEquals(bytes, serializer.encodeToByteArray(message))
        } catch (e: SerializationException) {
            if (e.message == null) {
                System.err.println(bytes.toAsciiHexString())
                throw e
            }

            if (e.message!!.startsWith("Unexpected EOF") ||
                e.message == "Input stream is malformed: Varint too long (exceeded 64 bits)" ||
                e.message!!.endsWith("is not allowed as the protobuf field number in org.plan.research.Value, the input bytes may have been corrupted") ||
                checkCauses(e) { s -> s == "Unsupported start group or end group wire type: INVALID(-1)" } ||
                checkCauses(e) { s -> s != null && s.matches(Regex("Expected wire type .+, but found .+")) } ||
                checkCauses(e) {s -> s != null && s.startsWith("Unexpected negative length:")}
            ) return

            if (e.message!!.matches(Regex("""Serializer for subclass .+ is not found in the polymorphic scope of 'Value'.+""", RegexOption.DOT_MATCHES_ALL)) &&
                !e.message!!.startsWith("Serializer for subclass 'IntValue' is not found in the polymorphic scope of 'Value'.") &&
                !e.message!!.startsWith("Serializer for subclass 'LongValue' is not found in the polymorphic scope of 'Value'.") &&
                !e.message!!.startsWith("Serializer for subclass 'DoubleValue' is not found in the polymorphic scope of 'Value'.") &&
                !e.message!!.startsWith("Serializer for subclass 'StringValue' is not found in the polymorphic scope of 'Value'.") &&
                !e.message!!.startsWith("Serializer for subclass 'NullValue' is not found in the polymorphic scope of 'Value'.") &&
                !e.message!!.startsWith("Serializer for subclass 'BooleanValue' is not found in the polymorphic scope of 'Value'.") &&
                !e.message!!.startsWith("Serializer for subclass 'EnumValue' is not found in the polymorphic scope of 'Value'.") &&
                !e.message!!.startsWith("Serializer for subclass 'DefaultValueNever' is not found in the polymorphic scope of 'Value'.") &&
                !e.message!!.startsWith("Serializer for subclass 'DefaultValueAlways' is not found in the polymorphic scope of 'Value'.") &&
                !e.message!!.startsWith("Serializer for subclass 'CompositeNullableValue' is not found in the polymorphic scope of 'Value'.") &&
                !e.message!!.startsWith("Serializer for subclass 'ObjectValue' is not found in the polymorphic scope of 'Value'.") &&
                !e.message!!.startsWith("Serializer for subclass 'ListValue' is not found in the polymorphic scope of 'Value'.") &&
                !e.message!!.startsWith("Serializer for subclass 'ArrayValue' is not found in the polymorphic scope of 'Value'.")
            ) return

            System.err.println(bytes.toAsciiHexString())
            throw e
        } catch (e: IllegalArgumentException) {
            // probably a bug?
            // not interesting??
            if (e.message != null &&
                (e.message == "Cannot read polymorphic value before its type token" ||
                 e.message!!.startsWith("Polymorphic value has not been read for class")) ||
                 e.message!!.matches(Regex("startIndex: .+ > endIndex: .+"))) return
            System.err.println(bytes.toAsciiHexString())
            throw e
        } catch (e: IndexOutOfBoundsException) {
            // not interesting??
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = MAX_DURATION)
    fun protoBufEncodeDecode(data: FuzzedDataProvider) {
        val serializer = ProtoBuf { encodeDefaults = data.consumeBoolean() }
        val message = data.generateValue(MAX_STR_LENGTH)
        val bytes: ByteArray
        try {
            bytes = serializer.encodeToByteArray(message)
            assertEquals(message, serializer.decodeFromByteArray<Value>(bytes))
        } catch (e: Exception) {
            System.err.println("[${message}]")
            throw e
        }
    }
}
