package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.html.TagConsumer
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.jsoup.Jsoup
import org.plan.research.Constants.MAX_DURATION
import org.plan.research.utils.DoubleTagConsumer
import org.plan.research.utils.TRef
import org.plan.research.utils.common
import org.plan.research.utils.genLambdaWithReceiver
import kotlin.test.assertEquals

object RandomDSL {
    @FuzzTest(maxDuration = MAX_DURATION)
    fun toStream(data: FuzzedDataProvider) {
        common(data, createHTML(data.consumeBoolean())) { }
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun toStreamParseJsoup(data: FuzzedDataProvider) {
        common(data, createHTML(data.consumeBoolean())) { html -> Jsoup.parse(html)!! }
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun toDom(data: FuzzedDataProvider) {
        common(data, createHTMLDocument()) { }
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun toDomSerializeParseJsoup(data: FuzzedDataProvider) {
        common(
            data,
            createHTMLDocument()
        ) { html -> Jsoup.parse(html.serialize(data.consumeBoolean())) }
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun toStreamVsToDom(data: FuzzedDataProvider) {
        val prettyPrint = data.consumeBoolean()
        common(
            data, DoubleTagConsumer(createHTML(prettyPrint), createHTMLDocument())
        ) { (text, doc) ->
            if (doc.attributes != null) {
                assertEquals(text, doc.serialize(prettyPrint))
            }
        }
    }
}
