package kotlinx.fuzz

import java.nio.charset.Charset
import kotlin.text.Charsets

interface KFuzzer {
    // Add regex-based string generation
    // Add DSL-like approach
    // Add random method call
    // Add consuming of an object (related to random method call)

    fun consumeBoolean(probability: Double): Boolean
    fun consumeNullableBoolean(trueProbability: Double, nullProbability: Double): Boolean?
    fun consumeBooleans(probability: Double, maxLength: Int): BooleanArray
    fun consumeNullableBooleans(trueProbability: Double, nullProbability: Double, maxLength: Int): BooleanArray?

    fun consumeByte(
        range: IntRange = Byte.MIN_VALUE..Byte.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM
    ): Byte

    fun consumeNullableByte(
        range: IntRange = Byte.MIN_VALUE..Byte.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM,
        nullProbability: Double
    ): Byte?

    fun consumeBytes(
        maxLength: Int,
        range: IntRange = Byte.MIN_VALUE..Byte.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM
    ): ByteArray

    fun consumeNullableBytes(
        maxLength: Int,
        range: IntRange = Byte.MIN_VALUE..Byte.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM,
        nullProbability: Double
    ): ByteArray?

    fun consumeRemainingAsByteArray(): ByteArray

    fun consumeShort(
        range: IntRange = Short.MIN_VALUE..Short.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM
    ): Short

    fun consumeNullableShort(
        range: IntRange = Short.MIN_VALUE..Short.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM,
        nullProbability: Double
    ): Short?

    fun consumeShorts(
        maxLength: Int,
        range: IntRange = Short.MIN_VALUE..Short.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM
    ): ShortArray

    fun consumeNullableShorts(
        maxLength: Int,
        range: IntRange = Short.MIN_VALUE..Short.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM,
        nullProbability: Double
    ): ShortArray?

    fun consumeInt(
        range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM
    ): Int

    fun consumeNullableInt(
        range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM,
        nullProbability: Double
    ): Int?

    fun consumeInts(
        maxLength: Int,
        range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM
    ): IntArray

    fun consumeNullableInts(
        maxLength: Int,
        range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM,
        nullProbability: Double
    ): IntArray?

    fun consumeLong(
        range: LongRange = Long.MIN_VALUE..Long.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM
    ): Long

    fun consumeNullableLong(
        range: LongRange = Long.MIN_VALUE..Long.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM,
        nullProbability: Double
    ): Long?

    fun consumeLongs(
        maxLength: Int,
        range: LongRange = Long.MIN_VALUE..Long.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM
    ): LongArray

    fun consumeNullableLongs(
        maxLength: Int,
        range: LongRange = Long.MIN_VALUE..Long.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM,
        nullProbability: Double
    ): LongArray?

    fun consumeFloat(
        range: ClosedFloatingPointRange<Float> = Float.MIN_VALUE..Float.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM
    ): Float

    fun consumeNullableFloat(
        range: ClosedFloatingPointRange<Float> = Float.MIN_VALUE..Float.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM,
        nullProbability: Double
    ): Float?

    fun consumeFloats(
        maxLength: Int,
        range: ClosedFloatingPointRange<Float> = Float.MIN_VALUE..Float.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM
    ): FloatArray

    fun consumeNullableFloats(
        maxLength: Int,
        range: ClosedFloatingPointRange<Float> = Float.MIN_VALUE..Float.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM,
        nullProbability: Double
    ): FloatArray?

    fun consumeDouble(
        range: ClosedFloatingPointRange<Double> = Double.MIN_VALUE..Double.MAX_VALUE,
        includeSpecialValues: Boolean = true,
        distribution: Distribution = Distribution.UNIFORM
    ): Double

    fun consumeNullableDouble(
        range: ClosedFloatingPointRange<Double> = Double.MIN_VALUE..Double.MAX_VALUE,
        includeSpecialValues: Boolean = true,
        distribution: Distribution = Distribution.UNIFORM,
        nullProbability: Double
    ): Double?

    fun consumeDoubles(
        maxLength: Int,
        range: ClosedFloatingPointRange<Double> = Double.MIN_VALUE..Double.MAX_VALUE,
        includeSpecialValues: Boolean = true,
        distribution: Distribution = Distribution.UNIFORM
    ): DoubleArray

    fun consumeNullableDoubles(
        maxLength: Int,
        range: ClosedFloatingPointRange<Double> = Double.MIN_VALUE..Double.MAX_VALUE,
        includeSpecialValues: Boolean = true,
        distribution: Distribution = Distribution.UNIFORM,
        nullProbability: Double
    ): DoubleArray?

    fun consumeChar(
        range: CharRange = Char.MIN_VALUE..Char.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM
    ): Char

    fun consumeNullableChar(
        range: CharRange = Char.MIN_VALUE..Char.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM,
        nullProbability: Double
    ): Char?

    fun consumeChars(
        maxLength: Int,
        range: CharRange = Char.MIN_VALUE..Char.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM
    ): CharArray

    fun consumeNullableChars(
        maxLength: Int,
        range: CharRange = Char.MIN_VALUE..Char.MAX_VALUE,
        distribution: Distribution = Distribution.UNIFORM,
        nullProbability: Double
    ): CharArray?

    fun consumeString(maxLength: Int, charset: Charset = Charsets.UTF_8): String
    fun consumeNullableString(maxLength: Int, charset: Charset = Charsets.UTF_8, nullProbability: Double): String?
    fun consumeRemainingAsString(charset: Charset = Charsets.UTF_8): String
    fun consumeAsciiString(maxLength: Int, charset: Charset = Charsets.UTF_8): String
    fun consumeNullableAsciiString(maxLength: Int, charset: Charset = Charsets.UTF_8, nullProbability: Double): String?
    fun consumeRemainingAsAsciiString(charset: Charset = Charsets.UTF_8): String
    fun consumeLetterString(maxLength: Int, charset: Charset = Charsets.UTF_8): String
    fun consumeNullableLetterString(maxLength: Int, charset: Charset = Charsets.UTF_8, nullProbability: Double): String?
    fun consumeRemainingAsLetterString(charset: Charset = Charsets.UTF_8): String


    fun <T> KFuzzer.pickValue(collection: Collection<T>): T {
        require(collection.isNotEmpty()) { "collection is empty" }
        return collection.elementAt(consumeInt(collection.indices))
    }

    fun <T> pickValue(array: Array<T>): T {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    fun pickValue(array: BooleanArray): Boolean {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    fun pickValue(array: ByteArray): Byte {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    fun pickValue(array: ShortArray): Short {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    fun pickValue(array: IntArray): Int {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    fun pickValue(array: LongArray): Long {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    fun pickValue(array: DoubleArray): Double {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    fun pickValue(array: FloatArray): Float {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    fun pickValue(array: CharArray): Char {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }
}