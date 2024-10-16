@file:Suppress("unused")

package org.plan.research.utils

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.html.TagConsumer
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import java.lang.reflect.InvocationTargetException

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