# Trophy list

* `kotlinx.serialization`
   * [JSON](https://github.com/Kotlin/kotlinx.serialization/issues/2885#issue-2742832022)
      * Invalid serialization due to unhandled conflict of `classDesctiptor` and `JsonNames`
      * Undocumented behaviour of `decodeToSequence` with `allowTrailingComma` option
      * Wrong error message in combination of `decodeToSequence` and `allowTrailingComma=false`
   * [CBOR](https://github.com/Kotlin/kotlinx.serialization/issues/2886)
     * Unhandled `IllegalStateException` when `decodeFromByteArray` reaches the end of the byte array
     * Unhandled `NegativeArraySizeException` when `decodeFromByteArray` encounters invalid data
     * Infinite recursion and `StackOverflowError` in `decodeFromByteArray` due to not handling reaching the end of the buffer
     * Unhandled `ArrayIndexOutOfBounds` when trying to skip an unknown element in the byte array
   * [Properties](https://github.com/Kotlin/kotlinx.serialization/issues/2887)
     * Undocumented behaviour with empty primitive arrays: they are not present in the resulting string
   * [ProtoBuf](https://github.com/Kotlin/kotlinx.serialization/issues/2888)
     * Some non-empty inputs are parsed as empty message, which breaks `a == deserialize(serialize(a))` invariant
     * Equal messages are encoded differently depending on the generic type
     * `a == deserialize(serialize(a))` invariant breaks for some not empty messages
     * Exception when trying to serialize a field with default value `null`
* `kotlinx.collections.immutable`
   * [Equal PersistentOrderedSets are not equal](https://github.com/Kotlin/kotlinx.collections.immutable/issues/204)
* `kotlinx.cli`
   * [Incorrect handling of "--" in GNU mode](https://github.com/Kotlin/kotlinx-cli/issues/106)
* `kotlinx-datetime`
  * [A number of bugs and inconsistencies with Java](https://github.com/Kotlin/kotlinx-datetime/issues/443)
    * `kotlinx.datetime.LocalDate.parse(s) vs LocalDate.Formats.ISO.parse(s)` have different behaviour with leading zeroes
    * `DatePeriod.parse` and `DateTimePeriod.parse` parsing "P" behaviour is inconsistent with ISO 8601
    * `kotlinx.datetime.DatePeriod` is always normalized unlike `java.time.Duration` (Intentional)
    * `kotlin.time.Duration.parseIsoString` with too many digits returns an infinite duration
    * `kotlin.time.Duration.parse("PT+-2H")` successfully parses -2 hours: not valid ISO 8601
    * Different periods between two dates in Java and Kotlin: bug in Java
* `kotlinx.io`
  * [{Source,Buffer}.indexOf(ByteString, Int) should return start index for empty ByteString](https://github.com/Kotlin/kotlinx-io/issues/423)
  * [Buffer.indexOf(ByteString) accepts negative indices](https://github.com/Kotlin/kotlinx-io/issues/422)
