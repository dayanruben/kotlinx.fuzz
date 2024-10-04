package org.plan.research

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.DecodeSequenceMode
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
        for (element in serializer.decodeToSequence<Value>(inputStream, DecodeSequenceMode.ARRAY_WRAPPED)) {
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
            for (element in serializer.decodeToSequence<Value>(inputStream, DecodeSequenceMode.ARRAY_WRAPPED)) {
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

    @Test
    fun `json name conflict`() {
        val serializer = Json {
            classDiscriminator = "third"
        }
        val value: Value = CompositeNullableValue(IntValue(0), IntValue(0), IntValue(0))
        val str = serializer.encodeToString(value)
        println(str)
    }

    @Test
    fun `json class descriptor name conflict`() {
        val serializer = Json {
            classDiscriminator = "THIS_IS_STATUS"
            classDiscriminatorMode = ClassDiscriminatorMode.POLYMORPHIC
        }
        val value: Value = CompositeNullableValue(
            StringValue("foo"),
            NullValue,
            NullValue
        )
        val str = serializer.encodeToString(value)
        val decodedValue = serializer.decodeFromString<Value>(str)
        assertTrue { value == decodedValue }
        assertTrue { value.status == decodedValue.status }
    }
}