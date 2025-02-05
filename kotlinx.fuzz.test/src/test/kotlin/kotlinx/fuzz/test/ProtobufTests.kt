@file:Suppress("UNCHECKED_CAST", "VARIABLE_WITH_REDUNDANT_INITIALIZER")

package kotlinx.fuzz.test

import kotlinx.fuzz.*
import org.junit.jupiter.api.Assertions.assertEquals
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

object ProtobufTests {
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


    private fun checkCauses(e: Throwable, pred: (String?) -> Boolean): Boolean {
        if (pred(e.message)) return true
        if (e.cause == null) return false
        return checkCauses(e.cause!!, pred)
    }

    private fun handleIllegalArgumentException(e: IllegalArgumentException) {
        if (e.message != null &&
            (e.message == "Cannot read polymorphic value before its type token" ||
                    e.message!!.startsWith("Polymorphic value has not been read for class")) ||
            e.message!!.matches(Regex("startIndex: .+ > endIndex: .+")) ||
            (e.message == "Polymorphic value has not been read for class null")
        ) return
        throw e
    }

    private fun handleSerializationException(e: SerializationException) {
        if (e.message == null) {
            throw e
        }

        if (e.message!!.startsWith("Unexpected EOF") ||
            e.message == "'null' is not allowed for not-null properties" ||
            e.message == "Input stream is malformed: Varint too long (exceeded 64 bits)" ||
            e.message == "Input stream is malformed: Varint too long (exceeded 32 bits)" ||
            e.message!!.matches(Regex(".+ is not allowed as the protobuf field number in .+, the input bytes may have been corrupted")) ||
            checkCauses(e) { s -> s == "Unsupported start group or end group wire type: INVALID(-1)" } ||
            checkCauses(e) { s -> s != null && s.matches(Regex("Expected wire type .+, but found .+")) } ||
            checkCauses(e) { s -> s != null && s.startsWith("Unexpected negative length:") } ||
            e.message!!.matches(Regex(".+ is not allowed as the protobuf field number in .+, the input bytes may have been corrupted")) ||
            e.message!!.matches(Regex("Unexpected .+ value: .+")) ||
            e.message == "Element 'value' is missing" ||
            e.message == "Element 'key' is missing" ||
            e.message == "Field 'value' is required for type with serial name 'org.plan.research.ProtobufMessageInt', but it was missing" ||
            checkCauses(e) { s -> s != null && s.matches(Regex(".+ is not among valid .+ enum proto numbers")) }
        ) return

        if (e.message!!.matches(
                Regex(
                    """Serializer for subclass .+ is not found in the polymorphic scope of .+""",
                    RegexOption.DOT_MATCHES_ALL
                )
            )
        ) return

        throw e
    }

    private fun <T> KFuzzer.generateProtobufMessage(
        clazz: Class<T>,
        maxStrLength: Int,
        maxDepth: Int
    ): ProtobufMessage<T> {
        assert(clazz in CLASSES)
        val res = ProtobufMessage(
            intFieldDefault = intOrNull(),
            intFieldFixed = intOrNull(),
            intFieldSigned = intOrNull(),
            floatField = floatOrNull(),
            doubleField = doubleOrNull(),
            stringField = asciiStringOrNull(maxStrLength),
            booleanField = booleanOrNull(),
            enumField = if (boolean()) pick(TestEnum.entries) else null,
            nestedMessageField = if (maxDepth > 0 && boolean()) generateProtobufMessage(
                clazz,
                maxStrLength,
                maxDepth - 1,
            ) else null,
            oneOfField = if (boolean()) if (boolean()) FirstOption(int()) else SecondOption(double()) else null,
            listField = generateProtobufList(clazz, maxStrLength, maxDepth - 1),
            packedListField = generateProtobufList(clazz, maxStrLength, maxDepth - 1),
            mapField = generateProtobufMap(clazz, maxStrLength, maxDepth - 1),
            packedMapField = generateProtobufMap(clazz, maxStrLength, maxDepth - 1),
        )

        if (boolean()) res.longField = long()

        return res
    }

    @Serializable
    data class ListInt(val value: List<Int>)

    @Serializable
    data class MapStringInt(val value: Map<String, Int>)

    @Serializable
    data class ProtobufMessageInt(val value: ProtobufMessage<Int>)

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

    private fun <T> KFuzzer.generateProtobufMap(
        clazz: Class<T>,
        maxStrLength: Int,
        maxDepth: Int
    ): Map<String, T> =
        when (clazz) {
            Int::class.java -> buildMap {
                repeat(int(0..maxStrLength)) {
                    put(string(maxStrLength), int() as T)
                }
            }

            Long::class.java -> buildMap {
                repeat(int(0..maxStrLength)) {
                    put(string(maxStrLength), long() as T)
                }
            }

            Float::class.java -> buildMap {
                repeat(int(0..maxStrLength)) {
                    put(string(maxStrLength), float() as T)
                }
            }

            Double::class.java -> buildMap {
                repeat(int(0..maxStrLength)) {
                    put(string(maxStrLength), double() as T)
                }
            }

            String::class.java -> buildMap {
                repeat(int(0..maxStrLength)) {
                    put(string(maxStrLength), asciiString(maxStrLength) as T)
                }
            }

            Boolean::class.java -> buildMap {
                repeat(int(0..maxStrLength)) {
                    put(string(maxStrLength), boolean() as T)
                }
            }

            TestEnum::class.java -> buildMap {
                repeat(int(0..maxStrLength)) {
                    put(
                        string(maxStrLength),
                        TestEnum.entries[int(0..TestEnum.entries.lastIndex)] as T
                    )
                }
            }

            OneOfType::class.java -> buildMap {
                repeat(int(0..maxStrLength)) {
                    put(
                        string(maxStrLength),
                        (if (boolean()) FirstOption(int()) else SecondOption(double())) as T
                    )
                }
            }

            ListInt::class.java -> if (maxDepth > 0) {
                buildMap {
                    repeat(int(0..maxStrLength)) {
                        put(
                            string(maxStrLength), ListInt(
                                generateProtobufList(
                                    Int::class.java,
                                    maxStrLength,
                                    maxDepth - 1
                                )
                            ) as T
                        )
                    }
                }
            } else emptyMap()

            MapStringInt::class.java -> if (maxDepth > 0) {
                buildMap {
                    repeat(int(0..maxStrLength)) {
                        put(
                            string(maxStrLength), MapStringInt(
                                generateProtobufMap(
                                    Int::class.java,
                                    maxStrLength,
                                    maxDepth - 1
                                )
                            ) as T
                        )
                    }
                }
            } else emptyMap()

            ProtobufMessageInt::class.java -> if (maxDepth > 0) {
                buildMap {
                    repeat(int(0..maxStrLength)) {
                        put(
                            string(maxStrLength), ProtobufMessageInt(
                                generateProtobufMessage(
                                    Int::class.java,
                                    maxStrLength,
                                    maxDepth - 1
                                )
                            ) as T
                        )
                    }
                }
            } else emptyMap()

            else -> error("Unexpected")
        }

    private fun <T> KFuzzer.generateProtobufList(
        clazz: Class<T>,
        maxStrLength: Int,
        maxDepth: Int
    ): List<T> =
        when (clazz) {
            Int::class.java -> MutableList(int(0..maxStrLength)) { int() as T }
            Long::class.java -> MutableList(int(0..maxStrLength)) { long() as T }
            Float::class.java -> MutableList(int(0..maxStrLength)) { float() as T }
            Double::class.java -> MutableList(int(0..maxStrLength)) { double() as T }
            String::class.java -> MutableList(int(0..maxStrLength)) { asciiString(maxStrLength) as T }
            Boolean::class.java -> MutableList(int(0..maxStrLength)) { boolean() as T }
            TestEnum::class.java -> MutableList(int(0..maxStrLength)) {
                TestEnum.entries[int(0..TestEnum.entries.lastIndex)] as T
            }

            OneOfType::class.java -> MutableList(int(0..maxStrLength)) {
                (if (boolean()) FirstOption(int()) else SecondOption(double())) as T
            }

            ListInt::class.java -> if (maxDepth > 0) {
                MutableList(int(0..maxStrLength)) {
                    ListInt(
                        generateProtobufList(
                            Int::class.java,
                            maxStrLength,
                            maxDepth - 1,
                        )
                    ) as T
                }
            } else emptyList()

            MapStringInt::class.java -> if (maxDepth > 0) {
                MutableList(int(0..maxStrLength)) {
                    MapStringInt(
                        generateProtobufMap(
                            Int::class.java,
                            maxStrLength,
                            maxDepth - 1
                        )
                    ) as T
                }
            } else emptyList()

            ProtobufMessageInt::class.java -> if (maxDepth > 0) {
                MutableList(int(0..maxStrLength)) {
                    ProtobufMessageInt(
                        generateProtobufMessage(
                            Int::class.java,
                            maxStrLength,
                            maxDepth - 1
                        )
                    ) as T
                }
            } else emptyList()

            else -> error("Unexpected")
        }


    @Serializable
    @OptIn(ExperimentalSerializationApi::class)
    data class ProtobufMessage<T>(
        @ProtoType(ProtoIntegerType.DEFAULT)
        val intFieldDefault: Int?,
        @ProtoType(ProtoIntegerType.FIXED)
        val intFieldFixed: Int?,
        @ProtoType(ProtoIntegerType.SIGNED)
        val intFieldSigned: Int?,
        var longField: Long = 5,
        val floatField: Float?,
        val doubleField: Double?,
        val stringField: String?,
        val booleanField: Boolean?,
        val listField: List<T?> = emptyList(),
        @ProtoPacked val packedListField: List<T?> = emptyList(),
        val mapField: Map<String, T?> = emptyMap(),
        @ProtoPacked val packedMapField: Map<String, T?> = emptyMap(),
        val nestedMessageField: ProtobufMessage<T>?,
        val enumField: TestEnum?,
        @ProtoOneOf val oneOfField: OneOfType?,
    )

    @Serializable
    sealed interface OneOfType

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @JvmInline
    value class FirstOption(@ProtoNumber(1000) val valueInt: Int) : OneOfType

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @JvmInline
    value class SecondOption(@ProtoNumber(1001) val valueDouble: Double) : OneOfType

    @OptIn(ExperimentalSerializationApi::class)
    @KFuzzTest
    fun protoBufDecodeFromByteArray(data: KFuzzer) {
        val serializer = ProtoBuf { encodeDefaults = data.boolean() }
        when (CLASSES[data.int(0..CLASSES.lastIndex)]) {
            Int::class.java -> {
                val bytes = data.remainingAsByteArray()
                if (bytes.isEmpty()) return
                var message: ProtobufMessage<Int>? = null
                try {
                    message = serializer.decodeFromByteArray<ProtobufMessage<Int>>(bytes)
                    if (bytes.size > 100 && serializer.encodeToByteArray(message).size > 100) assertEquals(bytes, serializer.encodeToByteArray(message))
                } catch (e: SerializationException) {
                    handleSerializationException(e)
                } catch (e: IllegalArgumentException) {
                    handleIllegalArgumentException(e)
                } catch (_: IndexOutOfBoundsException) {
                } catch (_: MissingFieldException) {
                } catch (e: AssertionError) {
                    throw e
                }
            }

            Long::class.java -> {
                val bytes = data.remainingAsByteArray()
                if (bytes.isEmpty()) return
                var message: ProtobufMessage<Long>? = null
                try {
                    message = serializer.decodeFromByteArray<ProtobufMessage<Long>>(bytes)
                    if (bytes.size > 100 && serializer.encodeToByteArray(message).size > 100) assertEquals(bytes, serializer.encodeToByteArray(message))
                } catch (e: SerializationException) {
                    handleSerializationException(e)
                } catch (e: IllegalArgumentException) {
                    handleIllegalArgumentException(e)
                } catch (_: IndexOutOfBoundsException) {
                } catch (_: MissingFieldException) {
                } catch (e: AssertionError) {
                    throw e
                }
            }

            Float::class.java -> {
                val bytes = data.remainingAsByteArray()
                if (bytes.isEmpty()) return
                var message: ProtobufMessage<Float>? = null
                try {
                    message = serializer.decodeFromByteArray<ProtobufMessage<Float>>(bytes)
                    if (bytes.size > 100 && serializer.encodeToByteArray(message).size > 100) assertEquals(bytes, serializer.encodeToByteArray(message))
                } catch (e: SerializationException) {
                    handleSerializationException(e)
                } catch (e: IllegalArgumentException) {
                    handleIllegalArgumentException(e)
                } catch (_: IndexOutOfBoundsException) {
                } catch (_: MissingFieldException) {
                } catch (e: AssertionError) {
                    throw e
                }
            }

            Double::class.java -> {
                val bytes = data.remainingAsByteArray()
                if (bytes.isEmpty()) return
                var message: ProtobufMessage<Double>? = null
                try {
                    message = serializer.decodeFromByteArray<ProtobufMessage<Double>>(bytes)
                    if (bytes.size > 100 && serializer.encodeToByteArray(message).size > 100) assertEquals(bytes, serializer.encodeToByteArray(message))
                } catch (e: SerializationException) {
                    handleSerializationException(e)
                } catch (e: IllegalArgumentException) {
                    handleIllegalArgumentException(e)
                } catch (_: IndexOutOfBoundsException) {
                } catch (_: MissingFieldException) {
                } catch (e: AssertionError) {
                    throw e
                }
            }

            String::class.java -> {
                val bytes = data.remainingAsByteArray()
                if (bytes.isEmpty()) return
                var message: ProtobufMessage<String>? = null
                try {
                    message = serializer.decodeFromByteArray<ProtobufMessage<String>>(bytes)
                    if (bytes.size > 100 && serializer.encodeToByteArray(message).size > 100) assertEquals(bytes, serializer.encodeToByteArray(message))
                } catch (e: SerializationException) {
                    handleSerializationException(e)
                } catch (e: IllegalArgumentException) {
                    handleIllegalArgumentException(e)
                } catch (_: IndexOutOfBoundsException) {
                } catch (_: MissingFieldException) {
                } catch (e: AssertionError) {
                    throw e
                }
            }

            Boolean::class.java -> {
                val bytes = data.remainingAsByteArray()
                if (bytes.isEmpty()) return
                var message: ProtobufMessage<Boolean>? = null
                try {
                    message = serializer.decodeFromByteArray<ProtobufMessage<Boolean>>(bytes)
                    if (bytes.size > 100 && serializer.encodeToByteArray(message).size > 100) assertEquals(bytes, serializer.encodeToByteArray(message))
                } catch (e: SerializationException) {
                    handleSerializationException(e)
                } catch (e: IllegalArgumentException) {
                    handleIllegalArgumentException(e)
                } catch (_: IndexOutOfBoundsException) {
                } catch (_: MissingFieldException) {
                } catch (e: AssertionError) {
                    throw e
                }
            }

            TestEnum::class.java -> {
                val bytes = data.remainingAsByteArray()
                if (bytes.isEmpty()) return
                var message: ProtobufMessage<TestEnum>? = null
                try {
                    message = serializer.decodeFromByteArray<ProtobufMessage<TestEnum>>(bytes)
                    if (bytes.size > 100 && serializer.encodeToByteArray(message).size > 100) assertEquals(bytes, serializer.encodeToByteArray(message))
                } catch (e: SerializationException) {
                    handleSerializationException(e)
                } catch (e: IllegalArgumentException) {
                    handleIllegalArgumentException(e)
                } catch (_: IndexOutOfBoundsException) {
                } catch (_: MissingFieldException) {
                } catch (e: AssertionError) {
                    throw e
                }
            }

            OneOfType::class.java -> {
                val bytes = data.remainingAsByteArray()
                if (bytes.isEmpty()) return
                var message: ProtobufMessage<OneOfType>? = null
                try {
                    message = serializer.decodeFromByteArray<ProtobufMessage<OneOfType>>(bytes)
                    if (bytes.size > 100 && serializer.encodeToByteArray(message).size > 100) assertEquals(bytes, serializer.encodeToByteArray(message))
                } catch (e: SerializationException) {
                    handleSerializationException(e)
                } catch (e: IllegalArgumentException) {
                    handleIllegalArgumentException(e)
                } catch (_: IndexOutOfBoundsException) {
                } catch (_: MissingFieldException) {
                } catch (e: AssertionError) {
                    throw e
                }
            }

            ListInt::class.java -> {
                val bytes = data.remainingAsByteArray()
                if (bytes.isEmpty()) return
                var message: ProtobufMessage<ListInt>? = null
                try {
                    message = serializer.decodeFromByteArray<ProtobufMessage<ListInt>>(bytes)
                    if (bytes.size > 100 && serializer.encodeToByteArray(message).size > 100) assertEquals(bytes, serializer.encodeToByteArray(message))
                } catch (e: SerializationException) {
                    handleSerializationException(e)
                } catch (e: IllegalArgumentException) {
                    handleIllegalArgumentException(e)
                } catch (_: IndexOutOfBoundsException) {
                } catch (_: MissingFieldException) {
                } catch (e: AssertionError) {
                    throw e
                }
            }

            MapStringInt::class.java -> {
                val bytes = data.remainingAsByteArray()
                if (bytes.isEmpty()) return
                var message: ProtobufMessage<MapStringInt>? = null
                try {
                    message = serializer.decodeFromByteArray<ProtobufMessage<MapStringInt>>(bytes)
                    if (bytes.size > 100 && serializer.encodeToByteArray(message).size > 100) assertEquals(bytes, serializer.encodeToByteArray(message))
                } catch (e: SerializationException) {
                    handleSerializationException(e)
                } catch (e: IllegalArgumentException) {
                    handleIllegalArgumentException(e)
                } catch (_: IndexOutOfBoundsException) {
                } catch (_: MissingFieldException) {
                } catch (e: AssertionError) {
                    throw e
                }
            }

            ProtobufMessageInt::class.java -> {
                val bytes = data.remainingAsByteArray()
                if (bytes.isEmpty()) return
                var message: ProtobufMessage<ProtobufMessageInt>? = null
                try {
                    message = serializer.decodeFromByteArray<ProtobufMessage<ProtobufMessageInt>>(bytes)
                    if (bytes.size > 100 && serializer.encodeToByteArray(message).size > 100) assertEquals(bytes, serializer.encodeToByteArray(message))
                } catch (e: SerializationException) {
                    handleSerializationException(e)
                } catch (e: IllegalArgumentException) {
                    handleIllegalArgumentException(e)
                } catch (_: IndexOutOfBoundsException) {
                } catch (_: MissingFieldException) {
                } catch (e: AssertionError) {
                    throw e
                }
            }
        }
    }
}