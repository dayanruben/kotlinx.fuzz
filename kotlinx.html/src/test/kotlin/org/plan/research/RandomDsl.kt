@file:Suppress("unused")

package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.jsoup.Jsoup
import org.plan.research.Constants.MAX_DURATION
import org.plan.research.utils.TRef
import org.plan.research.utils.genLambdaWithReceiver

val EMPTY_HTML = createHTML().html {}

private inline fun logHtmlOnException(block: () -> Unit) = try {
    block()
} catch (e: Throwable) {
    System.err.println(TRef.root.prettyPrint())
    throw e
}

class RandomDsl {
    @FuzzTest(maxDuration = MAX_DURATION)
    fun toStream(data: FuzzedDataProvider) {
        TRef.root = TRef("html")
        val html_lambda = genLambdaWithReceiver(data, TRef.root)
        logHtmlOnException {
            val s = createHTML(data.consumeBoolean()).html(null, html_lambda)
            Jsoup.parse(s)!!
        }
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun toDom(data: FuzzedDataProvider) {
        TRef.root = TRef("html")
        val html_lambda = genLambdaWithReceiver(data, TRef.root)
        logHtmlOnException {
            val doc = createHTMLDocument().html(null, html_lambda)
        }
    }
}
