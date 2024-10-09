@file:Suppress("unused")

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
import org.plan.research.utils.TRef
import org.plan.research.utils.genLambda
import org.w3c.dom.Document

val EMPTY_HTML = createHTML().html {}

class Bruh {
    @FuzzTest
    fun newMethods(data: FuzzedDataProvider) {
        TRef.root = TRef("html")
        val html_lambda = genLambda(data, TRef.root) as HTML.() -> Unit
        createHTML().html(null, html_lambda)
    }
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
