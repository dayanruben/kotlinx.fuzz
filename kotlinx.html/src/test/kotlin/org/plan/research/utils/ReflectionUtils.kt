@file:Suppress("UNREACHABLE_CODE")

package org.plan.research.utils

import kotlinx.html.HtmlTagMarker
import kotlinx.html.Tag
import kotlinx.html.TagConsumer
import kotlinx.html.consumers.PredicateResult
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.prepend
import kotlinx.html.html
import org.junit.jupiter.api.Assertions.assertTrue
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction
import kotlin.reflect.typeOf
import kotlin.test.Test


@Suppress("MemberVisibilityCanBePrivate")
object ReflectionUtils {

    val ref: Reflections = Reflections("kotlinx.html", *Scanners.entries.toTypedArray())
    val methods: List<KFunction<*>> =
        ref.getMethodsAnnotatedWith(HtmlTagMarker::class.java).map { it.kotlinFunction!! }
    val tagExtensions: List<KFunction<*>> = methods.filter {
        it.extensionReceiverParameter?.type?.isSubtypeOf(typeOf<Tag>()) == true
    }
    val tagToMethods: MutableMap<KClass<*>, MutableList<KFunction<*>>>
    val consumerMethodsReturnsTag: List<KFunction<*>>
    val tagToSetters: Map<KClass<out Tag>, List<KMutableProperty.Setter<*>>>
    val enumToValues: Map<KClass<out Enum<*>>, Array<out Enum<*>>>
    val tags: List<KClass<out Tag>> = ref.getSubTypesOf(Tag::class.java).map { it.kotlin }
    val tagNames = tags.map { it }

    val predicateResults = PredicateResult.entries.toTypedArray()


    init {
        val tagToExactMethods = tagExtensions.groupBy {
            it.extensionReceiverParameter!!.type.jvmErasure
        }
        consumerMethodsReturnsTag = methods.filter {
            it.extensionReceiverParameter?.type?.jvmErasure == TagConsumer::class
        }

        tagToMethods = hashMapOf()
        tags.forEach { tag ->
            val superTypes = tags.filter { it.isSuperclassOf(tag) }
            tagToMethods[tag] =
                superTypes.flatMap { tagToExactMethods[it] ?: emptyList() }.toMutableList()
        }

        tagToSetters = tags.associateWithTo(hashMapOf()) {
            it.declaredMemberProperties.filterIsInstance<KMutableProperty<*>>().map { it.setter }
        }

        enumToValues = getEnumToValues(ref)
        val tagToMethodsExp = getTagToMethodsExp(ref)
        for (tag in tags) {
            tagToMethods[tag]!!.addAll(tagToMethodsExp[tag]!!)
        }

        validate()
    }

    @Test
    fun lol() {
        createHTMLDocument().html {}.prepend {}
        println(ref.getMethodsReturn(Any::class.java))
    }

    fun getEnumToValues(ref: Reflections): Map<KClass<out Enum<*>>, Array<out Enum<*>>> {
        val enumClasses = ref.getSubTypesOf(Enum::class.java).map { it.kotlin!! }
        return enumClasses.associateWithTo(hashMapOf()) { enumClass -> enumClass.java.enumConstants }
    }

    fun getTagToMethodsExp(ref: Reflections): Map<KClass<*>, Array<KFunction<*>>> {
        val tags = ref.getSubTypesOf(Tag::class.java).map { it.kotlin }
        return tags.associateWith { tag ->
            tag.declaredFunctions.toTypedArray()
        }
    }

    fun validate() {
        assertTrue(tagToMethods.values.all { it.distinct().size == it.size })
        assertTrue(consumerMethodsReturnsTag.distinct().size == consumerMethodsReturnsTag.size)
    }
}