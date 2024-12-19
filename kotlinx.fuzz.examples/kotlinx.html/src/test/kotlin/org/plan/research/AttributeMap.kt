@file:Suppress("UNUSED_VARIABLE")

package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.html.body
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.html
import org.plan.research.utils.TRef
import org.plan.research.utils.genTagConsumer
import org.w3c.dom.DOMException
import kotlin.test.assertEquals
import kotlin.test.assertFalse


object AttributeMap {
    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun randomOpsOnAttributes(data: FuzzedDataProvider): Unit = with(data) {
        TRef.root = TRef("html")
        val consumer = genTagConsumer(createHTMLDocument(), data)
        val oops = mutableListOf<MapOperation>()
        try {
            val doc = consumer.html(null) {
                val ops = data.consumeInt(0, 10)
                repeat(ops) {
                    val op = data.consumeMapOperation(this.attributes)
                    oops.add(op)
                    op.apply(this.attributes)
                }
                body { text("body") }
            }
        } catch (_: DOMException) {
        } catch (e: IllegalStateException) {
            if (e.message != "No tags were emitted") throw e
            Unit
        } catch (e: Throwable) {
            System.err.println(oops.joinToString("\n"))
            throw (e)
        }
    }
}

interface MapOperation {
    fun apply(map: MutableMap<String, String>)
}

data class Put(val key: String, val value: String) : MapOperation {
    override fun apply(map: MutableMap<String, String>) {
        map.put(key, value)
        assertEquals(map[key], value)
    }
}

data class Remove(val key: String) : MapOperation {
    override fun apply(map: MutableMap<String, String>) {
        map.remove(key)
        assertFalse(map.containsKey(key))
    }
}

data class PutAll(val newElems: Map<String, String>) : MapOperation {
    override fun apply(map: MutableMap<String, String>) {
        map.putAll(this.newElems)
        for ((key, value) in newElems) {
            assertEquals(map[key], value)
        }
    }
}

val EMPTY_MAP_OPERATIONS = listOf(PutAll::class, Put::class)
val MAP_OPERATIONS = listOf(Put::class, Remove::class, PutAll::class)

fun FuzzedDataProvider.consumeSafeString(len: Int): String {
    return CharArray(len) { consumeCharNoSurrogates() }.joinToString("")
}

fun FuzzedDataProvider.consumePutAll(): PutAll = consumeSafeString(40)
    .asSequence()
    .chunked(4)
    .map { it.joinToString("") }
    .filter { it.isNotEmpty() }
    .chunked(2)
    .associate { it[0] to it[1] }
    .let { PutAll(it) }


fun FuzzedDataProvider.consumeMapOperation(map: Map<String, String>): MapOperation {
    val operations = if (map.isEmpty()) EMPTY_MAP_OPERATIONS else MAP_OPERATIONS

    return when (val v = pickValue(operations)) {
        Put::class -> Put(consumeSafeString(10), consumeSafeString(10))
        Remove::class -> Remove(consumeSafeString(10))
        PutAll::class -> consumePutAll()
        else -> error("Unexpected operation: $v")
    }
}
