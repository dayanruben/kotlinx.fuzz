package org.plan.research.utils

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.jvmErasure

fun generateParameter(parameter: KParameter, data: FuzzedDataProvider): Any? {
    val paramType = parameter.type
    return when {
        paramType.isMarkedNullable && data.consumeBoolean() -> null
        else -> when (paramType.jvmErasure) {
            String::class, CharSequence::class -> data.consumeString(10)

//            Number::class -> if (data.consumeBoolean()) data.consumeLong() else data.consumeDouble()
            Boolean::class -> data.consumeBoolean()
            Byte::class -> data.consumeByte()
            Short::class -> data.consumeShort()
            Int::class -> data.consumeInt()
            Long::class -> data.consumeLong()

            UByte::class -> data.consumeByte().toUByte()
            UShort::class -> data.consumeShort().toUShort()
            UInt::class -> data.consumeInt().toUInt()
            ULong::class -> data.consumeLong().toULong()

            Float::class -> data.consumeFloat()
            Double::class -> data.consumeDouble()

            RawSink::class -> Buffer()
            RawSource::class -> Buffer()
            Buffer::class -> Buffer()

            ByteArray::class -> data.consumeBytes(10)
            ByteString::class -> ByteString(data.consumeBytes(10))

            Charset::class -> data.pickValue(CHARSETS)
            ByteBuffer::class -> {
                val bytes = data.consumeBytes(10)
                if (data.consumeBoolean()) {
                    ByteBuffer.wrap(bytes)
                } else {
                    return ByteBuffer.allocateDirect(bytes.size).apply { put(bytes) }
                }
            }

            OutputStream::class -> Buffer().asOutputStream()
            InputStream::class -> Buffer().asInputStream()


            else -> error("Unexpected parameter type: $paramType")
        }
    }
}

val CHARSETS = Charset.availableCharsets().values.toTypedArray()
val READ_AT_MOST_ARR: KFunction<Int> = Source::readAtMostTo
val READ_AT_MOST_BUF: KFunction<Long> = Source::readAtMostTo
val READ_BYTE_STRING: Source.(Int) -> ByteString = Source::readByteString

val INDEX_OF_BSTRING: Source.(ByteString, Long) -> Long = Source::indexOf
val INDEX_OF_ARR: Source.(Byte, Long, Long) -> Long = Source::indexOf

val READ_BYTE_ARRAY: Source.(Int) -> ByteArray = Source::readByteArray

fun KCallable<*>.defaultParams(data: FuzzedDataProvider, skipFirst: Boolean = true): Array<*> =
    parameters
        .drop(if (skipFirst) 1 else 0)
        .map { generateParameter(it, data) }
        .toTypedArray()

val WRITE_STRING: Sink.(String, Charset, Int, Int) -> Unit = Sink::writeString

inline fun KFunction<*>.generateArguments(
    data: FuzzedDataProvider,
    fallback: KFunction<*>.() -> Array<*> = { error("Unexpected method: $this") }
): Array<*> {
    val skipFirst = isExtension || isMember
    return if (parameters.size == 1 && skipFirst) emptyArray<Any?>()
    else {
        when (this) {
            READ_AT_MOST_ARR -> {
                val bytes = ByteArray(10)
                val first = data.consumeInt(0, bytes.size - 1)
                val last = data.consumeInt(first, bytes.size)
                arrayOf(bytes, first, last)
            }

            READ_AT_MOST_BUF -> arrayOf(Buffer(), data.consumeLong(0, 100))
            Source::skip -> arrayOf(data.consumeLong(0, 100))

            READ_BYTE_STRING -> arrayOf(data.consumeInt(0, 100))

            INDEX_OF_BSTRING -> arrayOf(
                generateParameter(parameters[1], data),
                data.consumeLong(0, 100)
            )

            INDEX_OF_ARR -> arrayOf(
                generateParameter(parameters[1], data),
                data.consumeLong(0, 100),
                data.consumeLong(0, 100)
            )

            Source::startsWith -> defaultParams(data, skipFirst)
            READ_BYTE_ARRAY -> arrayOf(data.consumeInt(0, 100))
            Source::readTo -> arrayOf(Buffer(), data.consumeInt(0, 100))

            Source::request -> arrayOf(data.consumeLong(0, 100))
            Source::readLineStrict -> arrayOf(data.consumeLong(0, 100))

            Source::transferTo -> defaultParams(data, skipFirst)
            WRITE_STRING -> {
                val s = data.consumeString(10)
                val charset = data.pickValue(CHARSETS)
                val startIndex = data.consumeInt(0, s.length)
                val endIndex = data.consumeInt(startIndex, s.length)
                arrayOf(s, charset, startIndex, endIndex)
            }

            else -> when (name) {
                "readAtMostTo" -> defaultParams(data, skipFirst)
                "readString" -> defaultParams(data, skipFirst)
                "readTo" -> defaultParams(data, skipFirst)
                else -> fallback()
            }
            //        else -> defaultParams()
        }
    }
}

fun copyArguments(
    args: Array<*>,
    data: FuzzedDataProvider,
) = Array(args.size) { i ->
    when (val arg = args[i]) {
        is RawSink -> Buffer()
        is ByteBuffer -> cloneByteBuffer(arg)
        is ByteArray -> arg.clone()
        else -> args[i]
    }
}

fun cloneByteBuffer(original: ByteBuffer): ByteBuffer {
    // Create clone with same capacity as original.
    val clone = if (original.isDirect())
        ByteBuffer.allocateDirect(original.capacity()) else
        ByteBuffer.allocate(original.capacity())

    // Create a read-only copy of the original.
    // This allows reading from the original without modifying it.
    val readOnlyCopy = original.asReadOnlyBuffer()

    // Flip and read from the original.
//    readOnlyCopy.flip()
    clone.put(readOnlyCopy)

    return clone.flip()
}