package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.html.BODY
import kotlinx.html.TagConsumer
import kotlinx.html.body
import kotlinx.html.html
import kotlinx.html.stream.appendHTML


class Misc {
    @FuzzTest(maxDuration = "2h")
    fun escapeAppend(data: FuzzedDataProvider): Unit = with (data){
        val s = data.consumeString(1000)
        val b = data.consumeString(1000)
        buildString {
            appendHTML().html{
                append(s)
                body {
                    text(b)
                }
            }
        }
    }
}