package org.plan.research.utils

import kotlinx.html.BODY
import kotlinx.html.HtmlTagMarker
import kotlinx.html.Tag
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction
import kotlin.reflect.typeOf

@Suppress("MemberVisibilityCanBePrivate")
object ReflectionUtils {

    val ref: Reflections = Reflections("kotlinx.html", *Scanners.entries.toTypedArray())
    val methods: List<KFunction<*>> =
        ref.getMethodsAnnotatedWith(HtmlTagMarker::class.java).map { it.kotlinFunction!! }
    val tagExtensions: List<KFunction<*>>
    val tagToMethods: MutableMap<KClass<*>, List<KFunction<*>>>
    val consumerMethods: List<KFunction<*>>
    val tagSetters: Map<KClass<out Tag>, List<KMutableProperty.Setter<*>>>

    init {
        Tag::class.declaredFunctions.forEach { println(it) }
        BODY::class.declaredMemberProperties.filterIsInstance<KMutableProperty<*>>().map {
            it.setter
        }

        val tags = ref.getSubTypesOf(Tag::class.java).map { it.kotlin }
        tagExtensions = methods.filter {
            it.extensionReceiverParameter?.type?.isSubtypeOf(typeOf<Tag>()) == true
        }

        val tagToExactMethods = tagExtensions.groupBy {
            it.extensionReceiverParameter!!.type.jvmErasure
        }
        consumerMethods = methods.filter {
            it.extensionReceiverParameter?.type?.jvmErasure == Tag::class
        }

        tagToMethods = mutableMapOf()
        tags.forEach { tag ->
            val superTypes = tags.filter { it.isSuperclassOf(tag) }
            tagToMethods[tag] = superTypes.flatMap { tagToExactMethods[it] ?: emptyList() }
        }

        tagSetters = tags.associateWith {
            it.declaredMemberProperties.filterIsInstance<KMutableProperty<*>>().map { it.setter }
        }
    }
}