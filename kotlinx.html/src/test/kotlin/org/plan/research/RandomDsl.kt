@file:Suppress("unused")

package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.html.TagConsumer
import kotlinx.html.consumers.PredicateResult
import kotlinx.html.consumers.delayed
import kotlinx.html.consumers.filter
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import kotlinx.html.fieldSet
import kotlinx.html.figure
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.jsoup.Jsoup
import org.plan.research.Constants.MAX_DURATION
import org.plan.research.utils.TRef
import org.plan.research.utils.genTagConsumer
import org.plan.research.utils.genTagConsumerCall
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test

val EMPTY_HTML = createHTML().html {}

inline fun logHtmlOnException(block: () -> Unit): Unit = try {
    block()
} catch (e: IllegalStateException) {
    if (e.message != "No tags were emitted") throw e
    Unit
} catch (e: InvocationTargetException) {
    if (e.cause is IllegalStateException) {
        if (e.cause?.message != "No tags were emitted") throw e.cause!!
        Unit
    } else {
        throw e
    }
} catch (e: Throwable) {
    System.err.println(TRef.root.prettyPrint())
    throw e
}

inline fun <T : Any> common(
    data: FuzzedDataProvider, initialConsumer: TagConsumer<T>, block: (T) -> Unit
) {
    TRef.root = TRef("notatag")
    val consumerLambda = genTagConsumerCall<T>(data, TRef.root)
    val consumer = genTagConsumer(initialConsumer, data)
    logHtmlOnException {
        val res = consumer.consumerLambda()
        block(res)
    }
}

object AAA {
    @Test
    fun a() {
        println(createHTMLDocument().figure {}.serialize())

        println(Jsoup.parse(createHTMLDocument().fieldSet {}.serialize()))
    }
}

object RandomDsl {

    @FuzzTest(maxDuration = MAX_DURATION)
    fun toDom(data: FuzzedDataProvider) {
        common(data, createHTMLDocument()) { }
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun withFilter(data: FuzzedDataProvider) {
        val predicateResults = PredicateResult.entries.toTypedArray()
        common(data, createHTMLDocument().filter { data.pickValue(predicateResults) }) {}
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun withFilterDelayed(data: FuzzedDataProvider) {
        val predicateResults = PredicateResult.entries.toTypedArray()
        common(data, createHTMLDocument().filter { data.pickValue(predicateResults) }.delayed()) {}
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun delayed(data: FuzzedDataProvider) {
        common(data, createHTMLDocument().delayed()) {}
    }
}
