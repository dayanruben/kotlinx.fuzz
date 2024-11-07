package org.plan.research.utils

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
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
            String::class -> data.consumeString(10)
//            Number::class -> if (data.consumeBoolean()) data.consumeLong() else data.consumeDouble()
            Boolean::class -> data.consumeBoolean()
            Byte::class -> data.consumeByte()
            Short::class -> data.consumeShort()
            Int::class -> data.consumeInt()
            Long::class -> data.consumeLong()

            RawSink::class -> Buffer()
            Buffer::class -> Buffer()

            ByteArray::class -> data.consumeBytes(10)
            ByteString::class -> ByteString(data.consumeBytes(10))

            Charset::class -> data.pickValue(CHARSETS)
            ByteBuffer::class -> if (data.consumeBoolean()) {
                ByteBuffer.wrap(data.consumeBytes(10))
            } else {
                val bytes = data.consumeBytes(10)
                return ByteBuffer.allocateDirect(bytes.size).apply { put(bytes) }
            }


            else -> error("Unexpected parameter type: $paramType")
        }
    }
}

val CHARSETS = Charset.availableCharsets().values.toTypedArray()
val READ_AT_MOST_ARR: KFunction<Int> = Source::readAtMostTo
val READ_AT_MOST_BUF: KFunction<Long> = Source::readAtMostTo

fun KCallable<*>.generateArguments(data: FuzzedDataProvider, skipFirst: Boolean = true): Array<*> {
    return when (this) {
        READ_AT_MOST_ARR -> {
            val bytes = ByteArray(10)
            val first = data.consumeInt(0, bytes.size - 1)
            val last = data.consumeInt(first, bytes.size)
            arrayOf(bytes, first, last)
        }

        READ_AT_MOST_BUF -> arrayOf(Buffer(), data.consumeLong(0, 100))
        Source::skip -> arrayOf(data.consumeLong(0, 100))
        else -> parameters
            .drop(if (skipFirst) 1 else 0)
            .map { generateParameter(it, data) }
            .toTypedArray()
    }
}

fun KCallable<*>.copyArguments(
    args: Array<*>,
    data: FuzzedDataProvider,
    skipFirst: Boolean = true
) = Array(args.size) { i ->
    when (val arg = args[i]) {
        is RawSink -> Buffer()
        is ByteBuffer -> cloneByteBuffer(arg)
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
    readOnlyCopy.flip()
    clone.put(readOnlyCopy)

    return clone
}