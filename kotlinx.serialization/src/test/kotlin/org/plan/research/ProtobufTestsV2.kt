package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.serialization.protobuf.*
import kotlinx.serialization.*

fun <T> FuzzedDataProvider.generateProtobufMessage(
    clazz: Class<T>,
    maxStrLength: Int,
    maxDepth: Int
): ProtobufMessage<T> {
    assert(clazz in CLASSES)
    return ProtobufMessage(
        intFieldDefault = if (consumeBoolean()) consumeInt() else null,
        intFieldFixed = if (consumeBoolean()) consumeInt() else null,
        intFieldSigned = if (consumeBoolean()) consumeInt() else null,
        longField = if (consumeBoolean()) consumeLong() else null,
        floatField = if (consumeBoolean()) consumeFloat() else null,
        doubleField = if (consumeBoolean()) consumeDouble() else null,
        stringField = if (consumeBoolean()) consumeAsciiString(maxStrLength) else null,
        booleanField = if (consumeBoolean()) consumeBoolean() else null,
        enumField = if (consumeBoolean()) TestEnum.entries[consumeInt(0, TestEnum.entries.lastIndex)] else null,
        nestedMessageField = if (maxDepth > 0 && consumeBoolean()) generateProtobufMessage(
            clazz,
            maxStrLength,
            maxDepth - 1,
        ) else null,
        oneOfField = if (consumeBoolean()) if (consumeBoolean()) FirstOption(consumeInt()) else SecondOption(consumeInt()) else null,
        listField = generateProtobufList(clazz, maxStrLength, maxDepth - 1),
        packedListField = generateProtobufList(clazz, maxStrLength, maxDepth - 1),
        mapField = generateProtobufMap(clazz, maxStrLength, maxDepth - 1),
        packedMapField = generateProtobufMap(clazz, maxStrLength, maxDepth - 1),
    )
}


@Serializable
data class ListInt (val value: List<Int>)

@Serializable
data class MapStringInt (val value: Map<String, Int>)

@Serializable
data class ProtobufMessageInt (val value: ProtobufMessage<Int>)

private val CLASSES = listOf(
    Int::class.java,
    Long::class.java,
    Float::class.java,
    Double::class.java,
    String::class.java,
    Boolean::class.java,
    TestEnum::class.java,
    OneOfType::class.java,
    ListInt::class.java,
    MapStringInt::class.java,
    ProtobufMessageInt::class.java
)

fun <T> FuzzedDataProvider.generateProtobufMap(
    clazz: Class<T>,
    maxStrLength: Int,
    maxDepth: Int
): Map<String, T> =
    when (clazz) {
        Int::class.java -> buildMap {
            repeat(consumeInt(0, maxStrLength)) {
                put(consumeString(maxStrLength), consumeInt() as T)
            }
        }

        Long::class.java -> buildMap {
            repeat(consumeInt(0, maxStrLength)) {
                put(consumeString(maxStrLength), consumeLong() as T)
            }
        }

        Float::class.java -> buildMap {
            repeat(consumeInt(0, maxStrLength)) {
                put(consumeString(maxStrLength), consumeFloat() as T)
            }
        }

        Double::class.java -> buildMap {
            repeat(consumeInt(0, maxStrLength)) {
                put(consumeString(maxStrLength), consumeDouble() as T)
            }
        }

        String::class.java -> buildMap {
            repeat(consumeInt(0, maxStrLength)) {
                put(consumeString(maxStrLength), consumeAsciiString(maxStrLength) as T)
            }
        }

        Boolean::class.java -> buildMap {
            repeat(consumeInt(0, maxStrLength)) {
                put(consumeString(maxStrLength), consumeBoolean() as T)
            }
        }

        TestEnum::class.java -> buildMap {
            repeat(consumeInt(0, maxStrLength)) {
                put(
                    consumeString(maxStrLength),
                    TestEnum.entries[consumeInt(0, TestEnum.entries.lastIndex)] as T
                )
            }
        }

        OneOfType::class.java -> buildMap {
            repeat(consumeInt(0, maxStrLength)) {
                put(
                    consumeString(maxStrLength),
                    (if (consumeBoolean()) FirstOption(consumeInt()) else SecondOption(consumeInt())) as T
                )
            }
        }

        ListInt::class.java -> if (maxDepth > 0) {
            buildMap {
                repeat(consumeInt(0, maxStrLength)) {
                    put(
                        consumeString(maxStrLength), ListInt(generateProtobufList(
                            Int::class.java,
                            maxStrLength,
                            maxDepth - 1
                        )) as T
                    )
                }
            }
        } else emptyMap()

        MapStringInt::class.java -> if (maxDepth > 0) {
            buildMap {
                repeat(consumeInt(0, maxStrLength)) {
                    put(
                        consumeString(maxStrLength), MapStringInt(generateProtobufMap(
                            Int::class.java,
                            maxStrLength,
                            maxDepth - 1
                        )) as T
                    )
                }
            }
        } else emptyMap()

        ProtobufMessageInt::class.java -> if (maxDepth > 0) {
            buildMap {
                repeat(consumeInt(0, maxStrLength)) {
                    put(
                        consumeString(maxStrLength), ProtobufMessageInt(generateProtobufMessage(
                            Int::class.java,
                            maxStrLength,
                            maxDepth - 1
                        )) as T
                    )
                }
            }
        } else emptyMap()

        else -> error("Unexpected")
    }

fun <T> FuzzedDataProvider.generateProtobufList(
    clazz: Class<T>,
    maxStrLength: Int,
    maxDepth: Int
): List<T> =
    when (clazz) {
        Int::class.java -> MutableList(consumeInt(0, maxStrLength)) { consumeInt() as T }
        Long::class.java -> MutableList(consumeInt(0, maxStrLength)) { consumeLong() as T }
        Float::class.java -> MutableList(consumeInt(0, maxStrLength)) { consumeFloat() as T }
        Double::class.java -> MutableList(consumeInt(0, maxStrLength)) { consumeDouble() as T }
        String::class.java -> MutableList(consumeInt(0, maxStrLength)) { consumeAsciiString(maxStrLength) as T }
        Boolean::class.java -> MutableList(consumeInt(0, maxStrLength)) { consumeBoolean() as T }
        TestEnum::class.java -> MutableList(consumeInt(0, maxStrLength)) {
            TestEnum.entries[consumeInt(0, TestEnum.entries.lastIndex)] as T
        }

        OneOfType::class.java -> MutableList(consumeInt(0, maxStrLength)) {
            (if (consumeBoolean()) FirstOption(consumeInt()) else SecondOption(consumeInt())) as T
        }

        ListInt::class.java -> if (maxDepth > 0) {
            MutableList(consumeInt(0, maxStrLength)) {
                ListInt(generateProtobufList(
                    Int::class.java,
                    maxStrLength,
                    maxDepth - 1,
                )) as T
            }
        } else emptyList()

        MapStringInt::class.java -> if (maxDepth > 0) {
            MutableList(consumeInt(0, maxStrLength)) {
                MapStringInt(generateProtobufMap(
                    Int::class.java,
                    maxStrLength,
                    maxDepth - 1
                )) as T
            }
        } else emptyList()

        ProtobufMessageInt::class.java -> if (maxDepth > 0) {
            MutableList(consumeInt(0, maxStrLength)) {
                ProtobufMessageInt(generateProtobufMessage(
                    Int::class.java,
                    maxStrLength,
                    maxDepth - 1
                )) as T
            }
        } else emptyList()

        else -> error("Unexpected")
    }

const val COMPILE_TIME_RANDOM_0 = 1234
const val COMPILE_TIME_RANDOM_1: Int = (COMPILE_TIME_RANDOM_0 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_2: Int = (COMPILE_TIME_RANDOM_1 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_3: Int = (COMPILE_TIME_RANDOM_2 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_4: Int = (COMPILE_TIME_RANDOM_3 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_5: Int = (COMPILE_TIME_RANDOM_4 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_6: Int = (COMPILE_TIME_RANDOM_5 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_7: Int = (COMPILE_TIME_RANDOM_6 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_8: Int = (COMPILE_TIME_RANDOM_7 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_9: Int = (COMPILE_TIME_RANDOM_8 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_10: Int = (COMPILE_TIME_RANDOM_9 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_11: Int = (COMPILE_TIME_RANDOM_10 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_12: Int = (COMPILE_TIME_RANDOM_11 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_13: Int = (COMPILE_TIME_RANDOM_12 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_14: Int = (COMPILE_TIME_RANDOM_13 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_15: Int = (COMPILE_TIME_RANDOM_14 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_16: Int = (COMPILE_TIME_RANDOM_15 * 31 + 12345) % 10007
const val COMPILE_TIME_RANDOM_17: Int = (COMPILE_TIME_RANDOM_16 * 31 + 12345) % 10007

@Serializable
data class ProtobufMessage<T> @OptIn(ExperimentalSerializationApi::class) constructor(
    @ProtoNumber(COMPILE_TIME_RANDOM_1)
    @ProtoType(ProtoIntegerType.DEFAULT)
    val intFieldDefault: Int?,
    @ProtoNumber(COMPILE_TIME_RANDOM_2)
    @ProtoType(ProtoIntegerType.FIXED)
    val intFieldFixed: Int?,
    @ProtoNumber(COMPILE_TIME_RANDOM_3)
    @ProtoType(ProtoIntegerType.SIGNED)
    val intFieldSigned: Int?,
    @ProtoNumber(COMPILE_TIME_RANDOM_4)
    val longField: Long?,
    @ProtoNumber(COMPILE_TIME_RANDOM_5)
    val floatField: Float?,
    @ProtoNumber(COMPILE_TIME_RANDOM_6)
    val doubleField: Double?,
    @ProtoNumber(COMPILE_TIME_RANDOM_7)
    val stringField: String?,
    @ProtoNumber(COMPILE_TIME_RANDOM_8)
    val booleanField: Boolean?,
    @ProtoNumber(COMPILE_TIME_RANDOM_9)
    val listField: List<T?> = emptyList(),
    @ProtoNumber(COMPILE_TIME_RANDOM_10)
    @ProtoPacked
    val packedListField: List<T?> = emptyList(),
    @ProtoNumber(COMPILE_TIME_RANDOM_11)
    val mapField: Map<String, T?> = emptyMap(),
    @ProtoNumber(COMPILE_TIME_RANDOM_12)
    @ProtoPacked
    val packedMapField: Map<String, T?> = emptyMap(),
    @ProtoNumber(COMPILE_TIME_RANDOM_13)
    val nestedMessageField: ProtobufMessage<T>?,
    @ProtoNumber(COMPILE_TIME_RANDOM_14)
    val enumField: TestEnum?,
    @ProtoNumber(COMPILE_TIME_RANDOM_15)
    @ProtoOneOf val oneOfField: OneOfType?,
)

@Serializable
sealed interface OneOfType

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JvmInline
value class FirstOption(@ProtoNumber(COMPILE_TIME_RANDOM_16) val value: Int) : OneOfType

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JvmInline
value class SecondOption(@ProtoNumber(COMPILE_TIME_RANDOM_17) val value: Int) : OneOfType

object ProtobufTestsV2 {
    private const val MAX_STR_LENGTH = 10
    private const val MAX_DEPTH = 3

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = "10s")
    fun testInt(data: FuzzedDataProvider) {
        val serializer = ProtoBuf { encodeDefaults = data.consumeBoolean() }
        val message = data.generateProtobufMessage(Int::class.java, MAX_STR_LENGTH, MAX_DEPTH)
        try {
            serializer.encodeToByteArray(message)
        } catch (e: /*Serialization*/Exception) {
            // if (e.javaClass.name != "kotlinx.serialization.cbor.internal.CborDecodingException") {
            System.err.println("[${message}]")
            throw e
            // }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = "10s")
    fun testLong(data: FuzzedDataProvider) {
        val serializer = ProtoBuf { encodeDefaults = data.consumeBoolean() }
        val message = data.generateProtobufMessage(Long::class.java, MAX_STR_LENGTH, MAX_DEPTH)
        try {
            serializer.encodeToByteArray(message)
        } catch (e: /*Serialization*/Exception) {
            // if (e.javaClass.name != "kotlinx.serialization.cbor.internal.CborDecodingException") {
            System.err.println("[${message}]")
            throw e
            // }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = "10s")
    fun testFloat(data: FuzzedDataProvider) {
        val serializer = ProtoBuf { encodeDefaults = data.consumeBoolean() }
        val message = data.generateProtobufMessage(Float::class.java, MAX_STR_LENGTH, MAX_DEPTH)
        try {
            serializer.encodeToByteArray(message)
        } catch (e: /*Serialization*/Exception) {
            // if (e.javaClass.name != "kotlinx.serialization.cbor.internal.CborDecodingException") {
            System.err.println("[${message}]")
            throw e
            // }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = "10s")
    fun testDouble(data: FuzzedDataProvider) {
        val serializer = ProtoBuf { encodeDefaults = data.consumeBoolean() }
        val message = data.generateProtobufMessage(Double::class.java, MAX_STR_LENGTH, MAX_DEPTH)
        try {
            serializer.encodeToByteArray(message)
        } catch (e: /*Serialization*/Exception) {
            // if (e.javaClass.name != "kotlinx.serialization.cbor.internal.CborDecodingException") {
            System.err.println("[${message}]")
            throw e
            // }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = "10s")
    fun testString(data: FuzzedDataProvider) {
        val serializer = ProtoBuf { encodeDefaults = data.consumeBoolean() }
        val message = data.generateProtobufMessage(String::class.java, MAX_STR_LENGTH, MAX_DEPTH)
        try {
            serializer.encodeToByteArray(message)
        } catch (e: /*Serialization*/Exception) {
            // if (e.javaClass.name != "kotlinx.serialization.cbor.internal.CborDecodingException") {
            System.err.println("[${message}]")
            throw e
            // }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = "10s")
    fun testEnum(data: FuzzedDataProvider) {
        val serializer = ProtoBuf { encodeDefaults = data.consumeBoolean() }
        val message = data.generateProtobufMessage(TestEnum::class.java, MAX_STR_LENGTH, MAX_DEPTH)
        try {
            serializer.encodeToByteArray(message)
        } catch (e: /*Serialization*/Exception) {
            // if (e.javaClass.name != "kotlinx.serialization.cbor.internal.CborDecodingException") {
            System.err.println("[${message}]")
            throw e
            // }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = "10s")
    fun testBoolean(data: FuzzedDataProvider) {
        val serializer = ProtoBuf { encodeDefaults = data.consumeBoolean() }
        val message = data.generateProtobufMessage(Boolean::class.java, MAX_STR_LENGTH, MAX_DEPTH)
        try {
            serializer.encodeToByteArray(message)
        } catch (e: /*Serialization*/Exception) {
            // if (e.javaClass.name != "kotlinx.serialization.cbor.internal.CborDecodingException") {
            System.err.println("[${message}]")
            throw e
            // }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = "10s")
    fun testOneOf(data: FuzzedDataProvider) {
        val serializer = ProtoBuf { encodeDefaults = data.consumeBoolean() }
        val message = data.generateProtobufMessage(OneOfType::class.java, MAX_STR_LENGTH, MAX_DEPTH)
        try {
            serializer.encodeToByteArray(message)
        } catch (e: /*Serialization*/Exception) {
            // if (e.javaClass.name != "kotlinx.serialization.cbor.internal.CborDecodingException") {
            System.err.println("[${message}]")
            throw e
            // }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = "10s")
    fun testList(data: FuzzedDataProvider) {
        val serializer = ProtoBuf { encodeDefaults = data.consumeBoolean() }
        val message = data.generateProtobufMessage(ListInt::class.java, MAX_STR_LENGTH, MAX_DEPTH)
        try {
            serializer.encodeToByteArray(message)
        } catch (e: /*Serialization*/Exception) {
            // if (e.javaClass.name != "kotlinx.serialization.cbor.internal.CborDecodingException") {
            System.err.println("[${message}]")
            throw e
            // }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = "10s")
    fun testMap(data: FuzzedDataProvider) {
        val serializer = ProtoBuf { encodeDefaults = data.consumeBoolean() }
        val message = data.generateProtobufMessage(MapStringInt::class.java, MAX_STR_LENGTH, MAX_DEPTH)
        try {
            serializer.encodeToByteArray(message)
        } catch (e: /*Serialization*/Exception) {
            // if (e.javaClass.name != "kotlinx.serialization.cbor.internal.CborDecodingException") {
            System.err.println("[${message}]")
            throw e
            // }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = "10s")
    fun testProtobuf(data: FuzzedDataProvider) {
        val serializer = ProtoBuf { encodeDefaults = data.consumeBoolean() }
        val message = data.generateProtobufMessage(ProtobufMessageInt::class.java, MAX_STR_LENGTH, MAX_DEPTH)
        try {
            serializer.encodeToByteArray(message)
        } catch (e: /*Serialization*/Exception) {
            // if (e.javaClass.name != "kotlinx.serialization.cbor.internal.CborDecodingException") {
            System.err.println("[${message}]")
            throw e
            // }
        }
    }
}