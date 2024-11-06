package org.plan.research.utils

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asByteChannel
import kotlinx.io.asInputStream
import kotlinx.io.buffered
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.kotlinFunction
import kotlin.test.Test


@Suppress("MemberVisibilityCanBePrivate")
object ReflectionUtils {

    val ref: Reflections = Reflections("kotlinx.io", *Scanners.entries.toTypedArray())
    val sourceFunctions: Array<KFunction<*>> = getSourceCallables()

    private fun getSourceCallables(): Array<KFunction<*>> {
        val extensionFunctions = ref.getMethodsWithParameter(Source::class.java)
            .mapNotNull { it.kotlinFunction }
            .filter { it.isPublic }
            .filter { it.parameters.first().kind != KParameter.Kind.VALUE }
        val memberFunctions = Source::class.memberFunctions
        return (extensionFunctions + memberFunctions)
            .filter { it.isPublic }
            .filterNot { it.isBadSourceFunction() }
            .toTypedArray()
    }

    private fun KFunction<*>.isBadSourceFunction(): Boolean {
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
        val badNames = setOf<String>(
        )
        return this in bad || this.name in badNames
    }


    // can't create map with both methods and setters, because can't call setters after method


//    fun getSourceFunctions(): Array<KCallable<*>>{
//    }

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

    val KFunction<*>.isPublic: Boolean get() = visibility == KVisibility.PUBLIC
}
