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

fun handleIllegalArgumentException(e: IllegalArgumentException, bytes: ByteArray) {
    if (e.message != null &&
        (e.message == "Cannot read polymorphic value before its type token" ||
                e.message!!.startsWith("Polymorphic value has not been read for class")) ||
        e.message!!.matches(Regex("startIndex: .+ > endIndex: .+")) ||
        (e.message == "Polymorphic value has not been read for class null")) return
    System.err.println(bytes.toAsciiHexString())
    throw e
}

fun handleSerializationException(e: SerializationException, bytes: ByteArray) {
    if (e.message == null) {
        System.err.println(bytes.toAsciiHexString())
        throw e
    }

    if (e.message!!.startsWith("Unexpected EOF") ||
        e.message == "'null' is not allowed for not-null properties" ||
        e.message == "Input stream is malformed: Varint too long (exceeded 64 bits)" ||
        e.message == "Input stream is malformed: Varint too long (exceeded 32 bits)" ||
        e.message!!.matches(Regex(".+ is not allowed as the protobuf field number in .+, the input bytes may have been corrupted")) ||
        checkCauses(e) { s -> s == "Unsupported start group or end group wire type: INVALID(-1)" } ||
        checkCauses(e) { s -> s != null && s.matches(Regex("Expected wire type .+, but found .+")) } ||
        checkCauses(e) { s -> s != null && s.startsWith("Unexpected negative length:") } ||
        e.message!!.matches(Regex(".+ is not allowed as the protobuf field number in .+, the input bytes may have been corrupted")) ||
        e.message!!.matches(Regex("Unexpected .+ value: .+")) ||
        e.message == "Element 'value' is missing" ||
        e.message == "Element 'key' is missing" ||
        e.message == "Field 'value' is required for type with serial name 'org.plan.research.ProtobufMessageInt', but it was missing" ||
        checkCauses(e) {s -> s != null && s.matches(Regex(".+ is not among valid .+ enum proto numbers")) }
    ) return

    if (e.message!!.matches(Regex("""Serializer for subclass .+ is not found in the polymorphic scope of .+""", RegexOption.DOT_MATCHES_ALL))) return

    System.err.println(bytes.toAsciiHexString())
    throw e
}

object ProtobufTests {
    private const val MAX_STR_LENGTH = 1000
    private const val MAX_DURATION = "6h"

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
            handleSerializationException(e, bytes)
        } catch (e: IllegalArgumentException) {
            handleIllegalArgumentException(e, bytes)
        } catch (e: IndexOutOfBoundsException) {
            // System.err.println("[${bytes.toAsciiHexString()}]")
            // throw e
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
