package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.html.*
import kotlinx.html.consumers.measureTime
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import kotlinx.html.stream.appendHTML
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Test
import org.plan.research.utils.LambdaWrapper
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.w3c.dom.Document
import kotlin.random.Random.Default.nextInt
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction

val EMPTY_HTML = createHTML().html {}

class Bruh {

    companion object {
        val ref: Reflections = Reflections("kotlinx.html", *Scanners.entries.toTypedArray())
        val methods: List<KFunction<*>> = ref.getMethodsAnnotatedWith(HtmlTagMarker::class.java)
            .map { it.kotlinFunction!! }
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
                it.declaredMemberProperties
                    .filterIsInstance<KMutableProperty<*>>()
                    .map { it.setter }
            }
        }
    }

//    @Test
//    fun aaa() {
//
//    }

    fun FuzzedDataProvider.createLambda(receiverClass: KClass<*>): KFunction<*> {
        val num = nextInt(0, 3)
        val methods = tagToMethods[receiverClass]
        val calls = List(num) { pickValue(methods)!! }

        val a = LambdaWrapper {
            val rec = this
            calls.forEach { it.callWithData(rec, this@createLambda) }
        }
        return a
    }

    @FuzzTest
    fun lol(data: FuzzedDataProvider) {
        createHTMLDocument().html{
            data.createLambda(HTML::class).call(this)
        }
    }

    fun KFunction<*>.callWithData(receiver: Any, data: FuzzedDataProvider) {

        val firstParamType = parameters.first().type
        if (!firstParamType.isSubtypeOf(typeOf<Tag>())) {
//            error("Expected subtype of Tag, got $firstParamType")
        }
        val name = this.name
        val type = firstParamType.jvmErasure
        val paramTypes = parameters.drop(1).map {
            val jvmErasure = it.type.jvmErasure
            jvmErasure
        }
        println("Calling $name with type $type and params: ${paramTypes.joinToString(",")}")
        val args = paramTypes.map {
            when (it) {
                Enum::class -> error("enum")
                String::class -> data.consumeString(10)
                Function1::class -> data.createLambda(receiver::class)
                else -> error("Unexpected argument type: $it")
            }
        }
        println("calling $name, with receiver: ${receiver}")
        val a = this.parameters.map { it.type.jvmErasure }
        val b = (listOf(receiver) + args).map { it::class }
//        a.zip(b).firstOrNull { (param, arg) -> arg.isSubclassOf(param).not() }
//            ?.let { (param, arg) -> error("Excpected: $param, got: $arg") }
        this.call(receiver, *args.toTypedArray())
    }
}


fun HTML.goOn(data: FuzzedDataProvider): Unit = with(data) {

}

class TestTest {
    fun Document.noTagsWereEmitted() {
        try {
            this.getElementsByTagName("*")
        } catch (e: IllegalStateException) {
            if (e.message?.contains("No tags were emitted") == true) return
            throw e
        }
    }

    inline fun <T> noTagsWereEmitted(block: () -> T): T? = try {
        block()
    } catch (e: IllegalStateException) {
        if (e.message?.contains("No tags were emitted") == true) null
        else throw e
    }

    @Test
    fun a() {
        val a = createHTMLDocument()//.filter { if (it.tagName != "div") PASS else DROP }
        val doc = a.html {
            head {
                title { text("title") }
            }
            attributes["aaa"] = "bbb"
            if (this.attributes.isEmpty()) {
                return@html
            }
            body {
                text("a")
                div {
                    text("div")
                }
                a {
                    text("a")
                    href = "https://kotlinlang.org"
                }
                text("b")
            }

        }
        println(doc.serialize())
    }

    @Test
    fun b() {
        System.out.appendHTML().measureTime().html {
            head {
                title("Welcome page")
                script {}
            }
            body {
                div {
                    +"<<<special chars & entities goes here>>>"
                }
                div {
                    text("aaa")
                }
            }
        }.let {
            println()
            println("Generated in ${it.time} ms")
        }
    }

}
