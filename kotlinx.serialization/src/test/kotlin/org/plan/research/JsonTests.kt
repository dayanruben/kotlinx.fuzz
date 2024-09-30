package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.assertTrue

object JsonTests {
    private const val MAX_JSON_DEPTH = 10
    private const val MAX_STR_LENGTH = 100

    @FuzzTest(maxDuration = "2h")
    fun stringParsing(data: FuzzedDataProvider) {
        val jsonString = data.consumeRemainingAsAsciiString()
        try {
            val serializer = Json.Default
            serializer.parseToJsonElement(jsonString)
        } catch (e: SerializationException) {
            if (e.javaClass.name != "kotlinx.serialization.json.internal.JsonDecodingException") {
                System.err.println("\"$jsonString\"")
                throw e
            }
        }
    }

    @FuzzTest(maxDuration = "2h")
    fun jsonParsing(data: FuzzedDataProvider) {
        val jsonString = generateJson(data)
        try {
            val serializer = Json.Default
            serializer.parseToJsonElement(jsonString)
        } catch (e: SerializationException) {
            if (e.javaClass.name != "kotlinx.serialization.json.internal.JsonDecodingException") {
                System.err.println("\"$jsonString\"")
                throw e
            }
        }
    }

    private fun generateJson(data: FuzzedDataProvider): String = when {
        data.consumeBoolean() -> generateObject(data, 0)
        else -> generateArray(data, 0)
    }

    private fun generateObject(data: FuzzedDataProvider, depth: Int): String = buildString {
        appendLine("{")
        if (depth < MAX_JSON_DEPTH) {
            val elements = data.consumeInt(1, 100)
            repeat(elements) {
                val key = data.consumeAsciiString(MAX_STR_LENGTH)
                val value = generateElement(data, depth + 1)
                appendLine("\"$key\": $value${if (it < elements - 1) "," else ""}")
            }
        }
        appendLine("}")
    }

    private fun generateArray(data: FuzzedDataProvider, depth: Int): String = buildString {
        appendLine("[")
        if (depth < MAX_JSON_DEPTH) {
            val elements = data.consumeInt(1, 100)
            repeat(elements) {
                val value = generateElement(data, depth + 1)
                appendLine("$value${if (it < elements - 1) "," else ""}")
            }
        }
        appendLine("]")
    }

    private fun generateElement(data: FuzzedDataProvider, depth: Int): String = buildString {
        appendLine("{")
        val elements = data.consumeInt(1, 100)
        repeat(elements) {
            val key = data.consumeAsciiString(MAX_STR_LENGTH)
            val value = when (data.consumeInt(0, 6)) {
                0 -> data.consumeInt().toString()
                1 -> data.consumeLong().toString()
                2 -> data.consumeBoolean().toString()
                3 -> data.consumeDouble().toString()
                4 -> data.consumeAsciiString(MAX_STR_LENGTH)
                5 -> generateObject(data, depth + 1)
                else -> generateArray(data, depth + 1)
            }
            appendLine("\"$key\": \"$value\"${if (it < elements - 1) "," else ""}")
        }
        appendLine("}")
    }

    @FuzzTest(maxDuration = "2h")
    fun jsonEncodeAndDecode(data: FuzzedDataProvider) {
        val value = data.generateValue(MAX_STR_LENGTH)
        try {
            val jsoner = Json { allowSpecialFloatingPointValues = true }
            val json = jsoner.encodeToString(value)
            val decoded = jsoner.decodeFromString<Value>(json)
            assertTrue { isCorrect(value, decoded) }
        } catch (e: Throwable) {
            throw e
        }
    }

    @FuzzTest(maxDuration = "2h")
    fun jsonEncodeAndDecodeNested(data: FuzzedDataProvider) {
        val jsoner = Json { allowSpecialFloatingPointValues = true }
        val value = data.generateValue(MAX_STR_LENGTH)
        val strValueJson = jsoner.encodeToString(value)
        val newValue = CompositeNullableValue(
            StringValue(strValueJson), data.generateValue(MAX_STR_LENGTH), data.generateValue(MAX_STR_LENGTH)
        )
        try {
            val newValueJson = jsoner.encodeToString<Value>(newValue)
            val newValueDecoded = jsoner.decodeFromString<Value>(newValueJson)
            assertTrue { isCorrect(newValue, newValueDecoded) }

            val strValueDecoded = jsoner.decodeFromString<Value>(
                ((newValueDecoded as CompositeNullableValue).first as StringValue).value
            )
            assertTrue { isCorrect(value, strValueDecoded) }
        } catch (e: Throwable) {
            throw e
        }
    }


    private fun isCorrect(first: Value, second: Value): Boolean = when (first) {
        NullValue -> second is NullValue && first.status == second.status && first.status == "open"
        is BooleanValue -> first == second
        is IntValue -> first == second
        is LongValue -> first == second
        is DoubleValue -> first == second
        is ArrayValue -> second is ArrayValue && first.value.size == second.value.size && first.value.zip(second.value)
            .all { isCorrect(it.first, it.second) } && first.status == second.status && first.status == "open"

        is ListValue -> second is ListValue && first.value.size == second.value.size && first.value.zip(second.value)
            .all { isCorrect(it.first, it.second) } && first.status == second.status && first.status == "open"

        is ObjectValue -> second is ObjectValue && first.value.size == second.value.size && first.value.all {
            it.key in second.value && isCorrect(
                it.value,
                second.value[it.key]!!
            )
        } && first.status == second.status && first.status == "open"

        is StringValue -> second is StringValue && first.value == second.value && first.status == second.status && first.status == "open"
        is CompositeNullableValue -> second is CompositeNullableValue
                && if (first.first != null) isCorrect(first.first, second.first!!) else second.first == null
                && if (first.second != null) isCorrect(first.second, second.second!!) else second.second == null
                && if (first.third != null) isCorrect(first.third, second.third!!) else second.third == null
                && first.status == second.status && first.status == "open"

        is DefaultValueNever -> second is DefaultValueNever && isCorrect(
            first.value,
            second.value
        ) && first.status == second.status && first.status == "closed"

        is DefaultValueAlways -> second is DefaultValueAlways && isCorrect(
            first.value,
            second.value
        ) && first.status == second.status && first.status == "closed"
    }
}
