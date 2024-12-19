package org.plan.research.utils

import kotlinx.io.*
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction
import kotlin.test.Test


@Suppress("MemberVisibilityCanBePrivate")
object ReflectionUtils {

    val ref: Reflections = Reflections("kotlinx.io", *Scanners.entries.toTypedArray())
    val sourceFunctions: Array<KFunction<*>> = getCallables(Source::class) { isBadSourceFunction() }
    val sinkFunctions: Array<KFunction<*>> = getCallables(Sink::class) { isBadSinkFunction() }
    val bufferFunctions: Array<KFunction<*>> =
        getCallables(Buffer::class) { isBadBufferFunction() }


    private fun getCallables(
        klass: KClass<*>,
        isBad: KFunction<*>.() -> Boolean
    ): Array<KFunction<*>> {
        val extensionFunctions = ref.getMethodsWithParameter(klass.java)
            .mapNotNull { it.kotlinFunction }
        val memberFunctions = klass.memberFunctions
        return fixFunctions(klass, extensionFunctions + memberFunctions, isBad)
    }

    fun Array<KFunction<*>>.removeLongOps(): Array<KFunction<*>> =
        filter { it.name != "indexOf" }.toTypedArray()


    private fun fixFunctions(
        klass: KClass<*>,
        functions: Iterable<KFunction<*>>,
        isBad: KFunction<*>.() -> Boolean
    ): Array<KFunction<*>> = functions
        .filter { it.isPublic }
        .filter { it.parameters.first().type.jvmErasure == klass }
        .filter { it.parameters.first().kind != KParameter.Kind.VALUE }
        .filterNot { it.isBad() }
        .sortedBy { it.name }
        .shuffled(Random(42))
        .toTypedArray()


    @Test
    fun twf() {
        (ref.getMethodsWithParameter(Source::class.java).map { it.kotlinFunction!! }
            .filter { it.isPublic } +
                Source::class.memberFunctions.filter { it.isPublic }).let { println(it.size) } //.forEach { println(it) }
    }

    @Test
    fun sink() {
        (ref.getMethodsWithParameter(Sink::class.java).map { it.kotlinFunction!! }
            .filter { it.isPublic } +
                Sink::class.memberFunctions.filter { it.isPublic }).forEach { println(it) } //.let{println(it.size)} //.forEach { println(it) }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        extracted(this@ReflectionUtils.sourceFunctions)
        println()
        extracted(this@ReflectionUtils.sinkFunctions)
        println()
        extracted(this@ReflectionUtils.bufferFunctions)
        println()

        bufferFunctions.forEach { println(it) }
    }

    private fun extracted(functions: Array<KFunction<*>>) {
        println("Extensions: ${functions.filter { it.isExtension }.size}")
        println("Members: ${functions.filter { it.isMember }.size}")
        println("All: ${functions.size}")
    }

    val KFunction<*>.isPublic: Boolean get() = visibility == KVisibility.PUBLIC
}

@Suppress("OPT_IN_USAGE")
fun KFunction<*>.isBadSinkFunction(): Boolean {
    val bad = setOf<KFunction<*>>(
        Sink::buffered,
        Sink::close,

        Sink::hashCode,
        Sink::toString,
        Sink::equals,

        Sink::asByteChannel, // trivial, wont fuzz
        Sink::asOutputStream,

        Sink::writeToInternalBuffer,
    )
    return this in bad
}

fun KFunction<*>.isBadSourceFunction(): Boolean {
    val bad = setOf(
        Source::require,
        Source::peek,
        Source::buffered,
        Source::close,

        Source::asInputStream,
        Source::asByteChannel,

        Source::hashCode, // useless, inherited from Any
        Source::toString,
        Source::equals,
    )
    return this in bad
}

fun KFunction<*>.isBadBufferFunction(): Boolean {
    val bad = setOf(
        Buffer::toString,
        Buffer::hashCode,
        Buffer::equals,

        Buffer::peek, // fuzzed in PeekSourceTargets
        Buffer::require,
        Buffer::asByteChannel,
    )
    return this in bad
}

val KFunction<*>.isExtension: Boolean
    get() = parameters.first().kind == KParameter.Kind.EXTENSION_RECEIVER
val KFunction<*>.isMember: Boolean
    get() = parameters.first().kind == KParameter.Kind.INSTANCE