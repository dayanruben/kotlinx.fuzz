package org.plan.research

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object JsonReproductionTests {
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `json decode sequence cant parse array of enums with trailing comma`() {
        val string = """[{
    "type": "org.plan.research.EnumValue",
    "value": "SIXTH"
},]"""
        val inputStream = string.byteInputStream()
        val serializer = Json {
            allowTrailingComma = true
        }
        // works OK
        val directlyDecodedList = serializer.decodeFromString<List<Value>>(string)
        // `decodeToSequence` fails with `kotlinx.serialization.json.internal.JsonDecodingException`
        val values = mutableListOf<Value>()
        for (element in serializer.decodeToSequence<Value>(inputStream, DecodeSequenceMode.ARRAY_WRAPPED)) {
            values.add(element)
        }
        assertEquals(directlyDecodedList, values)
    }

    @OptIn(ExperimentalSerializationApi::class)
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
            for (element in serializer.decodeToSequence<Value>(inputStream, DecodeSequenceMode.ARRAY_WRAPPED)) {
                values.add(element)
            }
            //println(values.joinToString("\n"))
        } catch (e: SerializationException) {
            // Fails with "Unexpected JSON token at offset 47: Cannot read Json element because of unexpected end of the array ']'"
            // error message
            assertTrue {
                e.javaClass.name == "kotlinx.serialization.json.internal.JsonDecodingException"
                    && e.message.orEmpty().startsWith("Unexpected JSON token at offset")
                    && e.message.orEmpty().contains("Trailing comma before the end of JSON array at path:")
            }
        }
    }

    @Test
    fun `json class descriptor name conflict`() {
        val serializer = Json {
            classDiscriminator = "THIS_IS_STATUS"
        }
        val value: Value = CompositeNullableValue(
            StringValue("foo"),
            NullValue,
            NullValue
        )
        val str = serializer.encodeToString(value)
        val decodedValue = serializer.decodeFromString<Value>(str)
        assertTrue { value == decodedValue }
        // value.status == "open"
        // decodedValue.status == "org.plan.research.CompositeNullableValue"
        // test fail
        assertTrue { value.status == decodedValue.status }
    }
}