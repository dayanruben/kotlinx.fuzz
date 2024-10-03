package org.plan.research

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
object JsonReproductionTests {
    @Test
    fun `json decode sequence cant parse array of enums with trailing comma`() {
        val string = """[{
    "type": "org.plan.research.EnumValue",
    "value": "SIXTH"
},{
    "type": "org.plan.research.EnumValue",
    "value": "SEVENTH"
},{
    "type": "org.plan.research.EnumValue",
    "value": "FIFTH"
},{
    "type": "org.plan.research.EnumValue",
    "value": "SECOND"
},]"""
        val inputStream = string.byteInputStream()
        val serializer = Json {
            allowTrailingComma = true
        }
        val values = mutableListOf<Value>()
        for (element in serializer.decodeToSequence<Value>(inputStream)) {
            values.add(element)
        }
        println(values.joinToString("\n"))
    }

    @Test
    fun `json decode sequence fails with wrong message because of trailing comma`() {
        val string = """[{
    "type": "org.plan.research.NullValue"
},]"""
        val inputStream = string.byteInputStream()
        val serializer = Json {
            allowTrailingComma = false
        }
        try {
            val values = mutableListOf<Value>()
            for (element in serializer.decodeToSequence<Value>(inputStream)) {
                values.add(element)
            }
            println(values.joinToString("\n"))
        } catch (e: SerializationException) {
            assertTrue {
                e.javaClass.name == "kotlinx.serialization.json.internal.JsonDecodingException"
                    && e.message.orEmpty().startsWith("Unexpected JSON token at offset")
                    && e.message.orEmpty().contains("Trailing comma before the end of JSON array at path:")
            }
        }
    }
}