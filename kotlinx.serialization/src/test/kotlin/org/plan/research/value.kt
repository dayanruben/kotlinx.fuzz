package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.third_party.kotlin.reflect.jvm.internal.impl.resolve.constants.ArrayValue
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

private val CLASSES = listOf(
    NullValue::class,
    BooleanValue::class,
    IntValue::class,
    LongValue::class,
    DoubleValue::class,
    StringValue::class,
    ArrayValue::class,
    ListValue::class,
    ObjectValue::class,
    CompositeNullableValue::class,
    DefaultValueNever::class,
    DefaultValueAlways::class,
    EnumValue::class,
)

fun FuzzedDataProvider.generateValue(maxStrLength: Int): Value = when (CLASSES[consumeInt(0, CLASSES.lastIndex)]) {
    NullValue::class -> NullValue
    IntValue::class -> IntValue(consumeInt())
    LongValue::class -> LongValue(consumeLong())
    BooleanValue::class -> BooleanValue(consumeBoolean())
    DoubleValue::class -> DoubleValue(consumeDouble())
    StringValue::class -> StringValue(consumeRemainingAsAsciiString())
    ArrayValue::class -> ArrayValue(Array(consumeInt(0, maxStrLength)) { generateValue(maxStrLength) })
    ListValue::class -> ListValue(
        MutableList(consumeInt(0, maxStrLength)) { generateValue(maxStrLength) }
    )

    ObjectValue::class -> ObjectValue(
        buildMap {
            repeat(consumeInt(0, maxStrLength)) {
                val key = consumeString(maxStrLength)
                put(
                    key,
                    generateValue(maxStrLength)
                )
            }
        }
    )

    CompositeNullableValue::class -> {
        val fields = MutableList(3) {
            if (consumeBoolean()) generateValue(maxStrLength)
            else null
        }
        CompositeNullableValue(fields[0], fields[1], fields[2])
    }

    DefaultValueNever::class -> DefaultValueNever(generateValue(maxStrLength))
    DefaultValueAlways::class -> DefaultValueAlways(generateValue(maxStrLength))
    EnumValue::class -> EnumValue(TestEnum.entries[consumeInt(0, TestEnum.entries.lastIndex)])
    else -> error("Unexpected")
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
sealed class Value {
    @JsonNames(
        "status",
        "STATUS",
        "IS_OPEN"
    )
    var status = "open"
    val randomStr: String get() = status
}

@Serializable
data object NullValue : Value()

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
data class ArrayValue(val value: Array<Value>) : Value() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as org.plan.research.ArrayValue

        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

@Serializable
data class ListValue(val value: List<Value>) : Value()

@Serializable
@SerialName("obj")
data class ObjectValue(val value: Map<String, Value>) : Value()

@Serializable
data class CompositeNullableValue(
    val first: Value?,
    val second: Value?,
    val third: Value?,
) : Value()

@Serializable
data class DefaultValueNever @OptIn(ExperimentalSerializationApi::class) constructor(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val value: Value = IntValue(1),
) : Value() {
    init {
        status = "closed"
    }
}

@Serializable
data class DefaultValueAlways @OptIn(ExperimentalSerializationApi::class) constructor(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val value: Value = IntValue(1),
) : Value() {
    init {
        status = "closed"
    }
}

enum class TestEnum(val size: Int, val value: String) {
    FIRST(size = 1, value = "first"),
    SECOND(size = 2, value = "second"),
    THIRD(size = 3, value = "third"),
    FOURTH(size = 4, value = "fourth"),
    FIFTH(size = 5, value = "fifth"),
    SIXTH(size = 6, value = "sixth"),
    SEVENTH(size = 7, value = "seventh"),
    EIGHTH(size = 8, value = "eighth"),
    NINTH(size = 9, value = "ninth"),
    TENTH(size = 10, value = "tenth"),
    ELEVENTH(size = 11, value = "eleventh"),
}

@Serializable
data class EnumValue(
    val value: TestEnum
) : Value()

