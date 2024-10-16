package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.html.TagConsumer
import kotlinx.html.consumers.PredicateResult
import kotlinx.html.consumers.delayed
import kotlinx.html.consumers.filter
import kotlinx.html.stream.createHTML
import org.jsoup.Jsoup
import org.plan.research.Constants.MAX_DURATION

private fun template(data: FuzzedDataProvider, tagConsumer: TagConsumer<String>): Unit =
    common(data, tagConsumer) { html -> Jsoup.parse(html)!! }

object CreateHTMLRandomDSL {
    @FuzzTest(maxDuration = MAX_DURATION)
    fun just(data: FuzzedDataProvider) {
        template(data, createHTML(data.consumeBoolean()))
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun delayed(data: FuzzedDataProvider) {
        template(data, createHTML(data.consumeBoolean()).delayed())
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun filter(data: FuzzedDataProvider) {
        val a = PredicateResult.entries.toTypedArray()
        template(data, createHTML(data.consumeBoolean()).filter{data.pickValue(a)})
    }
}