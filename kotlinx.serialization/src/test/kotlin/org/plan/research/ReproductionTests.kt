package org.plan.research

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import kotlin.test.Test

object ReproductionTests {
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `json decode sequence cant parse array of enums`() {
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
}