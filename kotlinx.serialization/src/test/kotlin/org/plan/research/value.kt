package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.serialization.Serializable

fun FuzzedDataProvider.generateValue(maxStrLength: Int): Value = when (consumeInt(0, 10)) {
    0 -> IntValue(consumeInt())
    1 -> LongValue(consumeLong())
    2 -> BooleanValue(consumeBoolean())
    3 -> DoubleValue(consumeDouble())
    4 -> StringValue(consumeRemainingAsAsciiString())
    5, 6, 7 -> ListValue(
        MutableList(consumeInt(0, maxStrLength)) { generateValue(maxStrLength) }
    )
    else -> ObjectValue(
        buildMap {
            repeat(consumeInt(0, maxStrLength)) {
                val key = consumeAsciiString(maxStrLength)
                put(
                    key,
                    generateValue(maxStrLength)
                )
            }
        }
    )
}

@Serializable
sealed class Value

@Serializable
data class BooleanValue(val value: Boolean) : Value()

@Serializable
data class IntValue(val value: Int) : Value()
@Serializable
data class LongValue(val value: Long) : Value()

@Serializable
data class DoubleValue(val value: Double) : Value()

@Serializable
data class StringValue(val value: String) : Value()

@Serializable
data class ListValue(val value: List<Value>) : Value()

@Serializable
data class ObjectValue(val value: Map<String, Value>) : Value()
