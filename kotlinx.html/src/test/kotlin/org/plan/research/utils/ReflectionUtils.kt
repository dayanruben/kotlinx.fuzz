package org.plan.research.utils

import kotlinx.html.BODY
import kotlinx.html.HtmlTagMarker
import kotlinx.html.Tag
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

@Suppress("MemberVisibilityCanBePrivate")
object ReflectionUtils {

    val ref: Reflections = Reflections("kotlinx.html", *Scanners.entries.toTypedArray())
    val methods: List<KFunction<*>> =
        ref.getMethodsAnnotatedWith(HtmlTagMarker::class.java).map { it.kotlinFunction!! }
    val tagExtensions: List<KFunction<*>>
    val tagToMethods: MutableMap<KClass<*>, MutableList<KFunction<*>>>
    val consumerMethods: List<KFunction<*>>
    val tagSetters: Map<KClass<out Tag>, List<KMutableProperty.Setter<*>>>
    val enumToValues: Map<KClass<out Enum<*>>, Array<out Enum<*>>>
    val tagToMethodsExp: Map<KClass<*>, Array<KFunction<*>>>


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
            tagToMethods[tag] =
                superTypes.flatMap { tagToExactMethods[it] ?: emptyList() }.toMutableList()
        }

        tagSetters = tags.associateWith {
            it.declaredMemberProperties.filterIsInstance<KMutableProperty<*>>().map { it.setter }
        }

        enumToValues = getEnumToValues(ref)
        tagToMethodsExp = getTagToMethodsExp(ref)
        for (tag in tags) {
            tagToMethods[tag]!!.addAll(tagToMethodsExp[tag]!!)
        }

        for (tag in tags) {
            tagToMethods[tag]!!.addAll(tagSetters[tag]!!)
        }

        validate()
    }

    fun getEnumToValues(ref: Reflections): Map<KClass<out Enum<*>>, Array<out Enum<*>>> {
        val enumClasses = ref.getSubTypesOf(Enum::class.java).map { it.kotlin!! }
        val map = enumClasses.associateWith { enumClass -> enumClass.java.enumConstants }
        return map
    }

    fun getTagToMethodsExp(ref: Reflections): Map<KClass<*>, Array<KFunction<*>>> {
        val tags = ref.getSubTypesOf(Tag::class.java).map { it.kotlin }
        return tags.associateWith { tag ->
            tag.declaredFunctions.toTypedArray()
        }
    }

    fun validate() {
        assertTrue(tagToMethods.values.all { it.distinct().size == it.size })
        assertTrue(consumerMethods.distinct().size == consumerMethods.size)
    }
}