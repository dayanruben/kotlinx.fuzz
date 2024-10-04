package org.plan.research

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
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
            encodeDefaults = false
            ignoreUnknownKeys = true
            isLenient = true
            allowStructuredMapKeys = false
            prettyPrint = true
            explicitNulls = true
            prettyPrintIndent = "\n"
            coerceInputValues = true
            useArrayPolymorphism = false
            classDiscriminator = "THIS_IS_STATUS"
            allowSpecialFloatingPointValues = false
            useAlternativeNames = false
            namingStrategy = JsonNamingStrategy.SnakeCase
            decodeEnumsCaseInsensitive = true
            allowTrailingComma = true
            allowComments = false
            classDiscriminatorMode = ClassDiscriminatorMode.POLYMORPHIC
        }
        val strValue = """{

"THIS_IS_STATUS": "org.plan.research.DefaultValueAlways",

"status": "closed",

"value": {


"THIS_IS_STATUS": "org.plan.research.ArrayValue",


"value": [



{




"THIS_IS_STATUS": "org.plan.research.BooleanArrayValue",




"value": [





false,





true,





false,





true,





false,





false,





false,





false,





true,





true,





true,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false,





false




]



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



},



{




"THIS_IS_STATUS": "org.plan.research.NullValue"



}


]

}
}"""
        val value: Value = CompositeNullableValue(
            StringValue(strValue),
            NullValue,
            NullValue
        )
        val str = serializer.encodeToString(value)
        val decodedValue = serializer.decodeFromString<Value>(str)
        assertTrue { value == decodedValue }
        assertTrue { value.status == decodedValue.status }
    }
}