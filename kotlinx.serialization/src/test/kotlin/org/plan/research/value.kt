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
    ByteValue::class,
    CharValue::class,
    ShortValue::class,
    IntValue::class,
    LongValue::class,
    FloatValue::class,
    DoubleValue::class,
    StringValue::class,
    BooleanArrayValue::class,
    ByteArrayValue::class,
    CharArrayValue::class,
    ShortArrayValue::class,
    IntArrayValue::class,
    LongArrayValue::class,
    FloatArrayValue::class,
    DoubleArrayValue::class,
    ArrayValue::class,
    ListValue::class,
    ObjectValue::class,
    CompositeNullableValue::class,
    DefaultValueNever::class,
    DefaultValueAlways::class,
    EnumValue::class,
)

fun FuzzedDataProvider.generateValue(depth: Int = MAX_JSON_DEPTH): Value =
    if (depth == 0) NullValue
    else when (CLASSES[consumeInt(0, CLASSES.lastIndex)]) {
        NullValue::class -> NullValue
        BooleanValue::class -> BooleanValue(consumeBoolean())
        ByteValue::class -> ByteValue(consumeByte())
        CharValue::class -> CharValue(consumeByte().toInt().toChar())
        ShortValue::class -> ShortValue(consumeShort())
        IntValue::class -> IntValue(consumeInt())
        LongValue::class -> LongValue(consumeLong())
        FloatValue::class -> FloatValue(consumeFloat())
        DoubleValue::class -> DoubleValue(consumeDouble())
        StringValue::class -> StringValue(consumeRemainingAsAsciiString())
        BooleanArrayValue::class -> BooleanArrayValue(
            BooleanArray(
                consumeInt(
                    0,
                    MAX_STR_LENGTH
                )
            ) { consumeBoolean() })

        ByteArrayValue::class -> ByteArrayValue(ByteArray(consumeInt(0, MAX_STR_LENGTH)) { consumeByte() })
        CharArrayValue::class -> CharArrayValue(CharArray(consumeInt(0, MAX_STR_LENGTH)) {
            consumeByte().toInt().toChar()
        })

        ShortArrayValue::class -> ShortArrayValue(ShortArray(consumeInt(0, MAX_STR_LENGTH)) { consumeShort() })
        IntArrayValue::class -> IntArrayValue(IntArray(consumeInt(0, MAX_STR_LENGTH)) { consumeInt() })
        LongArrayValue::class -> LongArrayValue(LongArray(consumeInt(0, MAX_STR_LENGTH)) { consumeLong() })
        FloatArrayValue::class -> FloatArrayValue(FloatArray(consumeInt(0, MAX_STR_LENGTH)) { consumeFloat() })
        DoubleArrayValue::class -> DoubleArrayValue(DoubleArray(consumeInt(0, MAX_STR_LENGTH)) { consumeDouble() })
        ArrayValue::class -> ArrayValue(Array(consumeInt(0, MAX_STR_LENGTH)) { generateValue(depth - 1) })
        ListValue::class -> ListValue(
            MutableList(consumeInt(0, MAX_STR_LENGTH)) { generateValue(depth - 1) }
        )

        ObjectValue::class -> ObjectValue(
            buildMap {
                repeat(consumeInt(0, MAX_STR_LENGTH)) {
                    val key = consumeString(MAX_STR_LENGTH)
                    put(
                        key,
                        generateValue(depth - 1)
                    )
                }
            }
        )

        CompositeNullableValue::class -> {
            val fields = MutableList(3) {
                if (consumeBoolean()) generateValue(depth - 1)
                else null
            }
            CompositeNullableValue(fields[0], fields[1], fields[2])
        }

        DefaultValueNever::class -> DefaultValueNever(generateValue(depth - 1))
        DefaultValueAlways::class -> DefaultValueAlways(generateValue(depth - 1))
        EnumValue::class -> EnumValue(TestEnum.entries[consumeInt(0, TestEnum.entries.lastIndex)])
        else -> error("Unexpected")
    }

@OptIn(ExperimentalSerializationApi::class)
@Serializable
sealed class Value {
    @JsonNames(
        "THIS_IS_STATUS",
        "STATUS",
        "IS_OPEN"
    )
    var status = "open"

    @Suppress("unused")
    val randomStr: String get() = status
}

@Serializable
data object NullValue : Value()

@Serializable
data class BooleanValue(val value: Boolean) : Value()

@Serializable
data class ByteValue(val value: Byte) : Value()

@Serializable
data class CharValue(val value: Char) : Value()

@Serializable
data class ShortValue(val value: Short) : Value()

@Serializable
data class IntValue(val value: Int) : Value()

@Serializable
data class LongValue(val value: Long) : Value()

@Serializable
data class FloatValue(val value: Float) : Value()

@Serializable
data class DoubleValue(val value: Double) : Value()

@Serializable
data class StringValue(val value: String) : Value()

@Serializable
data class BooleanArrayValue(val value: BooleanArray) : Value() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BooleanArrayValue

        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

@Serializable
data class ByteArrayValue(val value: ByteArray) : Value() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteArrayValue

        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

@Serializable
data class CharArrayValue(val value: CharArray) : Value() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CharArrayValue

        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

@Serializable
data class ShortArrayValue(val value: ShortArray) : Value() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShortArrayValue

        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

@Serializable
data class IntArrayValue(val value: IntArray) : Value() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntArrayValue

        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

@Serializable
data class LongArrayValue(val value: LongArray) : Value() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LongArrayValue

        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

@Serializable
data class FloatArrayValue(val value: FloatArray) : Value() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FloatArrayValue

        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

@Serializable
data class DoubleArrayValue(val value: DoubleArray) : Value() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DoubleArrayValue

        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

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

