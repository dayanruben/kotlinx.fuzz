package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.assertTrue

/**
 * 1. Add comments
 * 2. `decodeToSequenceByReader` with different DecodeSequenceModes -- done
 * 3. Enums -- done
 * 4. JsonNames -- done
 * 5. Coerce values -- done
 * 6. Polymorphic
 * 7. `encodeToStream` / `encodeToSequence` -- done
 */

object JsonTests {
    private const val MAX_JSON_DEPTH = 10
    private const val MAX_STR_LENGTH = 100

    private fun isJsonDecodingException(e: Throwable): Boolean =
        e.javaClass.name == "kotlinx.serialization.json.internal.JsonDecodingException"

    private fun isJsonEncodingException(e: Throwable): Boolean =
        e.javaClass.name == "kotlinx.serialization.json.internal.JsonEncodingException"

    private fun isSpecialFloatingPointValueException(serializer: Json, e: Throwable): Boolean =
        (isJsonDecodingException(e) || isJsonEncodingException(e))
                && e.message.orEmpty().startsWith("Unexpected special floating-point value")
                && !serializer.configuration.allowSpecialFloatingPointValues

    @OptIn(ExperimentalSerializationApi::class)
    private fun FuzzedDataProvider.jsonSerializer(): Json = Json {
        encodeDefaults = consumeBoolean()
        ignoreUnknownKeys = consumeBoolean()
        isLenient = consumeBoolean()
        allowStructuredMapKeys = consumeBoolean()
        prettyPrint = consumeBoolean()
        explicitNulls = consumeBoolean()
        prettyPrintIndent = consumeString(MAX_STR_LENGTH)
        coerceInputValues = consumeBoolean()
        useArrayPolymorphism = consumeBoolean()
        classDiscriminator = consumeString(MAX_STR_LENGTH)
        allowSpecialFloatingPointValues = consumeBoolean()
        useAlternativeNames = consumeBoolean()
        namingStrategy = if (consumeBoolean()) JsonNamingStrategy.KebabCase else JsonNamingStrategy.SnakeCase
        decodeEnumsCaseInsensitive = consumeBoolean()
        allowTrailingComma = consumeBoolean()
        allowComments = consumeBoolean()
    }

    @FuzzTest(maxDuration = TEST_DURATION)
    fun stringParsing(data: FuzzedDataProvider) {
        val serializer = data.jsonSerializer()
        val jsonString = data.consumeRemainingAsAsciiString()
        val str: String
        try {
            val element: Any = when {
                data.consumeBoolean() -> serializer.parseToJsonElement(jsonString)
                else -> serializer.decodeFromString<Value>(jsonString)
            }
            str = serializer.encodeToString(element)
            assertTrue { jsonString == str }
        } catch (e: SerializationException) {
            if (!isJsonDecodingException(e)) {
                throw e
            }
        }
    }

    @FuzzTest(maxDuration = TEST_DURATION)
    fun jsonParsing(data: FuzzedDataProvider) {
        val serializer = data.jsonSerializer()
        val jsonString = generateJson(data)
        val str: String
        try {
            val element = serializer.parseToJsonElement(jsonString)
            str = serializer.encodeToString(element)
        } catch (e: SerializationException) {
            if (!isJsonDecodingException(e)) {
                throw e
            }
        }
    }

    @FuzzTest(maxDuration = TEST_DURATION)
    fun jsonEncodeAndDecode(data: FuzzedDataProvider) {
        val serializer = data.jsonSerializer()
        val value = data.generateValue(MAX_STR_LENGTH)
        try {
            val json = serializer.encodeToString(value)
            val decoded = serializer.decodeFromString<Value>(json)
            assertTrue { isCorrect(value, decoded) }
        } catch (e: Throwable) {
            if (isSpecialFloatingPointValueException(serializer, e)) {
                return
            } else {
                throw e
            }
        }
    }

    @FuzzTest(maxDuration = TEST_DURATION)
    fun jsonEncodeAndDecodeNested(data: FuzzedDataProvider) {
        val serializer = data.jsonSerializer()
        val value = data.generateValue(MAX_STR_LENGTH)
        val strValueJson = try {
            serializer.encodeToString(value)
        } catch (e: Throwable) {
            if (isSpecialFloatingPointValueException(serializer, e)) {
                return
            } else {
                throw e
            }
        }
        val newValue = CompositeNullableValue(
            StringValue(strValueJson), data.generateValue(MAX_STR_LENGTH), data.generateValue(MAX_STR_LENGTH)
        )
        try {
            val newValueJson = serializer.encodeToString<Value>(newValue)
            val newValueDecoded = serializer.decodeFromString<Value>(newValueJson)
            assertTrue { isCorrect(newValue, newValueDecoded) }

            val strValueDecoded = serializer.decodeFromString<Value>(
                ((newValueDecoded as CompositeNullableValue).first as StringValue).value
            )
            assertTrue { isCorrect(value, strValueDecoded) }
        } catch (e: Throwable) {
            if (isSpecialFloatingPointValueException(serializer, e)) {
                return
            } else {
                throw e
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = TEST_DURATION)
    fun streamParsing(data: FuzzedDataProvider) {
        val serializer = data.jsonSerializer()
        val jsonString = data.consumeRemainingAsAsciiString()
        val inputStream = ByteArrayInputStream(jsonString.toByteArray())
        val outputString = ByteArrayOutputStream()
        try {
            val element = serializer.decodeFromStream(JsonElement.serializer(), inputStream)
            serializer.encodeToStream(element, outputString)
//            assertTrue { jsonString == outputString.toString(Charsets.UTF_8.name()) }
        } catch (e: Throwable) {
            if (isSpecialFloatingPointValueException(serializer, e)
            ) {
                return
            } else if (isJsonDecodingException(e)) {
                return
            } else {
                throw e
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = TEST_DURATION)
    fun sequenceParsing(data: FuzzedDataProvider) {
        val serializer = data.jsonSerializer()
        val numberOfElements = data.consumeInt(1, 100)
        val elements = MutableList(numberOfElements) { data.generateValue(MAX_STR_LENGTH) }
        val addTrailingComma = data.consumeBoolean()
        val (str, encodeMode) = when (data.consumeBoolean()) {
            // true -> whitespace
            true -> buildString {
                for (element in elements) {
                    append(serializer.encodeToString(element))
                    append(data.consumeWhitespace())
                }
            } to DecodeSequenceMode.WHITESPACE_SEPARATED
            // false -> array
            false -> buildString {
                append("[")
                for ((index, element) in elements.withIndex()) {
                    append(serializer.encodeToString(element))
                    if (index < elements.lastIndex || addTrailingComma) {
                        append(",")
                    }
                }
                append("]")
            } to DecodeSequenceMode.ARRAY_WRAPPED
        }
        val stream = ByteArrayInputStream(str.toByteArray())
        val decodeMode = DecodeSequenceMode.entries[data.consumeInt(0, DecodeSequenceMode.entries.lastIndex)]
        try {
            var index = 0
            val sequence = serializer.decodeToSequence<Value>(stream, decodeMode)
            for (element in sequence) {
                assertTrue { decodeMode == DecodeSequenceMode.AUTO_DETECT || decodeMode == encodeMode }
                assertTrue { element == elements[index++] }
            }
        } catch (e: Throwable) {
            if (isSpecialFloatingPointValueException(serializer, e)) {
                return
            } else if (isJsonDecodingException(e)
                && e.message.orEmpty().startsWith("Expected JsonObject, but had JsonArray as the serialized body")
                && encodeMode == DecodeSequenceMode.ARRAY_WRAPPED && decodeMode == DecodeSequenceMode.WHITESPACE_SEPARATED
            ) {
                return
            } else if (isJsonDecodingException(e)
                && e.message.orEmpty().startsWith("Expected start of the array '[', but had 'EOF' instead at path:")
                && encodeMode == DecodeSequenceMode.WHITESPACE_SEPARATED && decodeMode == DecodeSequenceMode.ARRAY_WRAPPED
            ) {
                return
            } else if (isJsonDecodingException(e)
                && e.message.orEmpty().startsWith("Unexpected JSON token at offset")
                && e.message.orEmpty().contains("Trailing comma before the end of JSON array at path:")
                && !serializer.configuration.allowTrailingComma && encodeMode == DecodeSequenceMode.ARRAY_WRAPPED
            ) {
                return
            } else if (isJsonDecodingException(e)
                && e.message.orEmpty().startsWith("Unexpected JSON token at offset")
                && e.message.orEmpty().contains("Cannot read Json element because of unexpected end of the array ']'")
                && !serializer.configuration.allowTrailingComma && encodeMode == DecodeSequenceMode.ARRAY_WRAPPED
            ) {
                // possibly wrong error message?
                return
            } else if (isJsonDecodingException(e)
                && e.message.orEmpty().startsWith("Unexpected JSON token at offset")
                && e.message.orEmpty()
                    .contains("Cannot read Json element because of unexpected end of the array ']' at path")
                && serializer.configuration.allowTrailingComma && encodeMode == DecodeSequenceMode.ARRAY_WRAPPED
            ) {
                // bug, see ReproductionTests.`json decode sequence cant parse array of enums`
                return
            } else {
                throw e
            }
        }
    }

    private fun FuzzedDataProvider.consumeWhitespace(): Char = when (consumeInt(0, 3)) {
        0 -> ' '
        1 -> '\n'
        2 -> '\t'
        3 -> '\r'
        else -> error("Unexpected")
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


    private fun isCorrect(first: Value, second: Value): Boolean = when (first) {
        NullValue -> second is NullValue && first.status == second.status && first.status == "open"
        is BooleanValue -> first == second
        is ByteValue -> first == second
        is CharValue -> first == second
        is ShortValue -> first == second
        is IntValue -> first == second
        is FloatValue -> first == second
        is LongValue -> first == second
        is DoubleValue -> first == second
        is BooleanArrayValue -> first == second
        is ByteArrayValue -> first == second
        is CharArrayValue -> first == second
        is ShortArrayValue -> first == second
        is IntArrayValue -> first == second
        is FloatArrayValue -> first == second
        is LongArrayValue -> first == second
        is DoubleArrayValue -> first == second
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

        is EnumValue -> second is EnumValue && first.value == second.value && first.status == second.status && first.status == "open"
    }
}
