package org.plan.research.utils

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.bytestring.ByteString
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.jvmErasure

/*
import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.html.Tag
import kotlinx.html.TagConsumer
import kotlinx.html.consumers.*
import org.plan.research.utils.ReflectionUtils.predicateResults
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

fun KClass<*>.randomCalls(data: FuzzedDataProvider): List<KFunction<*>> {
    val methodsNum = data.consumeInt(0, 10)
    val settersNum = data.consumeInt(0, 2)

    val methods = ReflectionUtils.tagToMethods[this]!!
    val setters = ReflectionUtils.tagToSetters[this]!!

    return when {
        methods.isEmpty() && setters.isEmpty() -> emptyList()
        methods.isEmpty() -> List(settersNum) { data.pickValue(setters) }
        setters.isEmpty() -> List(methodsNum) { data.pickValue(methods) }
        else -> List(settersNum) { data.pickValue(setters) } + List(methodsNum) {
            data.pickValue(
                methods
            )
        }
    }
}


fun <T : Any> updateTagConsumer(
    initialConsumer: TagConsumer<T>, data: FuzzedDataProvider
): TagConsumer<T> = when (data.consumeInt(0, 5)) {
    0 -> initialConsumer.delayed()
    1 -> initialConsumer.measureTime().onFinalizeMap { res, partial -> res.result }
    2 -> initialConsumer.onFinalizeMap { res, partial -> res }
    3 -> initialConsumer.onFinalize { res, partial -> Unit }
    4 -> initialConsumer.trace { }
    5 -> initialConsumer.filter { data.pickValue(predicateResults) }
    else -> throw IllegalStateException()
}

fun <T : Any> genTagConsumer(
    initialConsumer: TagConsumer<T>,
    data: FuzzedDataProvider
): TagConsumer<T> {
    val num = data.consumeInt(0, 3)
    var tagConsumer = initialConsumer
    repeat(num) {
        tagConsumer = updateTagConsumer(tagConsumer, data)
    }
    return tagConsumer
}

private fun genArg(data: FuzzedDataProvider, paramType: KType, tref: TRef): Any? = when {
    paramType.isMarkedNullable && data.consumeBoolean() -> null
    paramType.isSubtypeOf(typeOf<Enum<*>?>()) -> data.pickValue(ReflectionUtils.enumToValues[paramType.jvmErasure]!!)
    paramType.isSubtypeOf(typeOf<Function<Unit>>()) -> genLambdaWithReceiver(data, tref)
    else -> when (paramType.jvmErasure) {
        String::class -> data.consumeString(10)
        Number::class -> if (data.consumeBoolean()) data.consumeLong() else data.consumeDouble()
        Boolean::class -> data.consumeBoolean()
        else -> error("Unexpected argument type: $paramType")
    }
}


fun genLambdaWithReceiver(data: FuzzedDataProvider, tref: TRef): Tag.() -> Unit = {
    val calls = this::class.randomCalls(data)
    calls.forEach {
        tref += it.name
        it.callWithData(this, data, tref.last())
    }
}


fun <T> genTagConsumerCall(data: FuzzedDataProvider, tref: TRef): TagConsumer<T>.() -> T {
    val method = data.pickValue(ReflectionUtils.consumerMethodsReturnsTag)
    val args = if (method.parameters.size > 1) {
        Array(method.parameters.size - 1) { i -> genArg(data, method.parameters[i + 1].type, tref) }
    } else {
        emptyArray()
    }
    return { method.call(this, *args) as T }
}

data class TRef(val tag: String, val children: MutableList<TRef> = mutableListOf()) {
    companion object {
        lateinit var root: TRef
        private const val SPACE = " "
    }

    fun last() = children.last()

    operator fun plusAssign(child: String) {
        children.add(TRef(child))
    }


    @Suppress("unused", "MemberVisibilityCanBePrivate")
    fun prettyPrint(indent: Int = 0): String = buildString {
        append("${SPACE.repeat(indent)}$tag {\n")
        children.forEach {
            append(it.prettyPrint(indent + 2))
        }
        append("${SPACE.repeat(indent)}}\n")
    }
}

fun KFunction<*>.callWithData(
    receiver: Tag,
    data: FuzzedDataProvider,
    tref: TRef
) {
//    assert(parameters.first().type.isSubtypeOf(typeOf<Tag>()))
    if (parameters.drop(1).all { it.isOptional } && data.consumeInt(0, 100) == 42) {
        callBy(mapOf(parameters.first() to receiver))
        return
    }

    val args = if (parameters.size > 1) {
        Array(parameters.size - 1) { i -> genArg(data, parameters[i + 1].type, tref) }
    } else {
        emptyArray()
    }
    call(receiver, *args)
}
*/


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

fun KCallable<*>.generateArguments(data: FuzzedDataProvider, skipFirst: Boolean = true): Array<*> {
    return parameters.drop(if (skipFirst) 1 else 0).map { generateParameter(it, data) }
        .toTypedArray()
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