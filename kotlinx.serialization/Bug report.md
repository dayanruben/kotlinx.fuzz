# Setup

We created the following class hierarchy for testing serialization library:

```kotlin
Value (org.plan.research)
    ArrayValue (org.plan.research)
    BooleanArrayValue (org.plan.research)
    BooleanValue (org.plan.research)
    ByteArrayValue (org.plan.research)
    ByteValue (org.plan.research)
    CharArrayValue (org.plan.research)
    CharValue (org.plan.research)
    CompositeNullableValue (org.plan.research)
    DefaultValueAlways (org.plan.research)
    DefaultValueNever (org.plan.research)
    DoubleArrayValue (org.plan.research)
    DoubleValue (org.plan.research)
    EnumValue (org.plan.research)
    FloatArrayValue (org.plan.research)
    FloatValue (org.plan.research)
    IntArrayValue (org.plan.research)
    IntValue (org.plan.research)
    ListValue (org.plan.research)
    LongArrayValue (org.plan.research)
    LongValue (org.plan.research)
    NullValue (org.plan.research)
    ObjectValue (org.plan.research)
    ShortArrayValue (org.plan.research)
    ShortValue (org.plan.research)
    StringValue (org.plan.research)
```

`Value` hierarchy tries to use most of the available serialization API and test it on all main data types available on Kotlin/JVM.
The exact implementation details are not important in most cases. We will highlight interesting implementation details whenever necessary.

# JSON

[Reporoducers](https://jetbrains.team/p/plan/repositories/kotlinx.fuzz/files/kotlinx.serialization/kotlinx.serialization/src/test/kotlin/org/plan/research/JsonReproductionTests.kt)

## 1\. Unhandled conflict of `classDesctiptor` config option and `JsonNames` annotation values

`Value` class has a property with custom JSON names specified:

```kotlin
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
```

If we specify `classDiscriminator` equal to one of the JSON names, it will create a name conflict that is not detected.
Moreover, this conflict will actually affect the serialization:

```kotlin
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
```

## 2\. Undocumented behaviour of `decodeToSequence` with `allowTrailingComma` option

While `allowTrailingComma=true` works well with `decodeFromString`, it does not affect `decodeToSequence` in any way.
This behaviour is not documented anywhere.

```kotlin
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
```

## 3\. Wrong error message in combination of `decodeToSequence` and `allowTrailingComma=false`

```kotlin
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
        println(values.joinToString("\n"))
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
```

# CBOR

CBOR bugs are mainly due to unhandled internal exceptions.

[Reporoducers](https://jetbrains.team/p/plan/repositories/kotlinx.fuzz/files/kotlinx.serialization/kotlinx.serialization/src/test/kotlin/org/plan/research/CborReproductionTests.kt)

## 1\. Unhandled `IllegalStateException`

Byte `126` is interpreted as "read a string of 30 characters"

```kotlin
@Test
fun `unhandled illegal state exception`() {
    val byteArray = byteArrayOf(126)
    val serializer = Cbor.Default
    // Fails with "java.lang.IllegalStateException: Unexpected EOF, available 0 bytes, requested: 30"
    assertThrows<SerializationException> {
        serializer.decodeFromByteArray<String>(byteArray)
    }
}
```

## 2\. Unhandled `NegativeArraySizeException`

Byte `40` is interpreted as an instruction to read `-9` bytes.

```kotlin
@Test
fun `unhandled negative array size exception`() {
    val byteArray = byteArrayOf(127, 40)
    val serializer = Cbor.Default
    // Fails with "java.lang.NegativeArraySizeException: -9"
    assertThrows<SerializationException> {
        serializer.decodeFromByteArray<String>(byteArray)
    }
}
```

## 3\. Unhandled `StackOverflowError`

Root of issue:

* `CborParser` class does not check for the end of the buffer
* `ByteArrayInput` returns `-1` on read if it has reached the end of the buffer
* `CborParser::readBytes` interprets this `-1` value as "read an indefinite number of bytes" and calls `CborParser::readIndefiniteLengthBytes` ; `CborParser::readIndefiniteLengthBytes` , meanwhile, calls `CborParser::readBytes` recursively

```kotlin
@Test
fun `unhandled stack overflow error`() {
    val byteArray = byteArrayOf(127, 0, 0)
    val serializer = Cbor.Default
    // Goes to infinite recursion:
    //   at kotlinx.serialization.cbor.internal.CborParser.readBytes(Decoder.kt:247)
    //   at kotlinx.serialization.cbor.internal.CborParser.readIndefiniteLengthBytes(Decoder.kt:514)
    assertThrows<SerializationException> {
        serializer.decodeFromByteArray<String>(byteArray)
    }
}
```

## 4\. Unhandled `ArrayIndexOutOfBounds`

Option `ignoreUnknownKeys=true` tells the parser to skip unknown elements.
Byte `122` at position 67 is interpreted as the start of the element and encodes its length of `-272646673`.
In an attempt to skip this element, the parser moved to `-272646673` bytes "ahead" in `ByteArrayInput` and sets the
current position to `-272646606`.

If `ignoreUnknownKeys=false`, this will fail with
`"kotlinx.serialization.cbor.internal.CborDecodingException: CborLabel unknown: 31 for obj(status: kotlin.String, value: kotlin.collections.LinkedHashMap)"`

```kotlin
@Test
fun `unhandled array index oob exception`() {
    val byteArray = byteArrayOf(
        -103, 7, 127, 127, -61, 111, 98, 106, 0, 0, -1, -66, -1, -9, -29, 47, 38, 38, 38, 38, 1, 38, 38, 38,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 38, 38, 38, 38, 38, 38, 111, 98, 106, -17, -65, -67, -17, -65, -67,
        -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, 122, -17, -65, -67, -17, -65, -67, -17,
        -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67,
        -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, 38, 38, 38, 38, 38,
        38, 38, 126, 126, 126, 38, 35, -128, -128, -128, -128, -128, -128, -128, -128, -128, 126, 126, 126,
        126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126,
        126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126,
        126, -67, -17, -65, -67, -17, 126, 126, 126, 126, 5, 0, 126, 126, 126, 126, 126, 126, 126, 126, 126,
        126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, -1, -1, -1, -1, -1, -1, -1,
        -1, 126, 126
    )
    val serializer = Cbor {
        ignoreUnknownKeys = true
    }
    // Fails with "java.lang.ArrayIndexOutOfBoundsException: Index -272646606 out of bounds for length 216"
    assertThrows<SerializationException> {
        serializer.decodeFromByteArray<Value>(byteArray)
    }
}
```

# Properties

[Reproducers](https://jetbrains.team/p/plan/repositories/kotlinx.fuzz/files/kotlinx.serialization/kotlinx.serialization/src/test/kotlin/org/plan/research/PropertiesReproductionTests.kt)

## 1\. Empty primitive arrays are not serialized

Empty primitive arrays are not present in any way in the encoded string.
Documentation does not specify behaviour in that case.

The same exception can be achieved with `null` value fields, but that behaviour is documented.

```kotlin
@OptIn(ExperimentalSerializationApi::class)
@Test
fun `missing field for empty primitive array`() {
    val value: Value = BooleanArrayValue(booleanArrayOf())
    val strMap = Properties.encodeToStringMap(value)
    // Fails with
    // "kotlinx.serialization.MissingFieldException: Field 'value' is required for type with serial name 'org.plan.research.BooleanArrayValue', but it was missing"
    val decodedValue = Properties.decodeFromStringMap<Value>(strMap)
    assertTrue { value == decodedValue }
}
```

# ProtoBuf

[Reproducers](https://jetbrains.team/p/plan/repositories/kotlinx.fuzz/files/kotlinx.serialization.protobuf/kotlinx.serialization/src/test/kotlin/org/plan/research/ProtoBufReproductionTests.kt)

## 0\. Setup

We will use the following structure of a Message:

```kotlin
@Serializable
sealed interface OneOfType

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JvmInline
value class FirstOption(val valueInt: Int) : OneOfType

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JvmInline
value class SecondOption(val valueDouble: Double) : OneOfType

@Serializable
data class ProtobufMessage<T> @OptIn(ExperimentalSerializationApi::class) constructor(
    @ProtoType(ProtoIntegerType.DEFAULT)
    val intFieldDefault: Int?,
    @ProtoType(ProtoIntegerType.FIXED)
    val intFieldFixed: Int?,
    @ProtoType(ProtoIntegerType.SIGNED)
    val intFieldSigned: Int?,
    var longField: Long? = 5,
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
```
It is slightly modified version of `Value`: added necessary annotations, unified types of lists and maps, added default value.

## 1\. Empty messages can be decoded from various sources

If we try to deserialize some strings we will get empty message even if input wasn't empty.

```kotlin
val bytes = byteArrayOf(9)
val message = ProtoBuf.decodeFromByteArray<ProtobufMessage<Int>>(bytes)
assertTrue { bytes.contentEquals(serializer.encodeToByteArray(message)) } // Fails
```

## 2\. Equal messages are encoded differently depending on type

If we try to serialize message with default values inclusion that is based on strings and message that is based on integers, we will get different results. And it works for all non-primitive and primitive types.

```kotlin
val messageInt = ProtobufMessage<Int>(
    intFieldDefault = null,
    intFieldFixed = null,
    intFieldSigned = null,
    // longField is 5 by default
    floatField = null,
    doubleField = null,
    stringField = null,
    booleanField = null,
    enumField = null,
    nestedMessageField = null,
    oneOfField = null,
    listField = emptyList(),
    packedListField = emptyList(),
    mapField = emptyMap(),
    packedMapField = emptyMap(),
)

val messageString = messageInt as ProtobufMessage<String>

val serializer = ProtoBuf { encodeDefaults = true }
val bytesForPrimitiveMessage = serializer.encodeToHexString<ProtobufMessage<Int>>(messageInt)
val bytesForNonPrimitiveMessages = serializer.encodeToHexString<ProtobufMessage<String>>(messageString)
assertTrue {bytesForPrimitiveMessage == bytesForNonPrimitiveMessages} // Fails
```

## 3\. Decoding-encoding transformation is not an identity

For some not empty messages we can find byte sequence that will be decoded as a message that encodes into a different byte array.

```kotlin
val bytes = byteArrayOf(-30, 125, 0, 125)
val serializer = ProtoBuf { encodeDefaults = true }
val message = serializer.decodeFromByteArray<ProtobufMessage<ProtobufMessageInt>>(bytes)
assertTrue { bytes.contentEquals(serializer.encodeToByteArray(message)) } // Fails
```

More examples that are not handful to be places here due to their size can be found in [Reproducers](https://jetbrains.team/p/plan/repositories/kotlinx.fuzz/files/kotlinx.serialization.protobuf/kotlinx.serialization/src/test/kotlin/org/plan/research/ProtoBufReproductionTests.kt)
