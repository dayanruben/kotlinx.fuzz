package kotlinx.fuzz.reproducer

import java.nio.charset.Charset
import kotlinx.fuzz.CharacterSet
import kotlinx.fuzz.DoubleRange
import kotlinx.fuzz.FloatRange
import kotlinx.fuzz.KFuzzer

class ValueReproducer(
    private val values: List<Any?>,
) : KFuzzer {
    private val iterator = values.iterator()

    override fun boolean(): Boolean = iterator.next() as Boolean

    override fun booleanOrNull(): Boolean? = iterator.next() as? Boolean?

    override fun booleans(maxLength: Int): BooleanArray = iterator.next() as BooleanArray

    override fun booleansOrNull(maxLength: Int): BooleanArray? = iterator.next() as? BooleanArray?

    override fun byte(range: IntRange): Byte = iterator.next() as Byte

    override fun byteOrNull(range: IntRange): Byte? = iterator.next() as? Byte?

    override fun bytes(maxLength: Int, range: IntRange): ByteArray = iterator.next() as ByteArray

    override fun bytesOrNull(maxLength: Int, range: IntRange): ByteArray? = iterator.next() as? ByteArray?

    override fun char(range: CharRange): Char = iterator.next() as Char

    override fun char(charset: Charset): Char = iterator.next() as Char

    override fun char(charset: CharacterSet): Char = iterator.next() as Char

    override fun charOrNull(range: CharRange): Char? = iterator.next() as? Char?

    override fun charOrNull(charset: Charset): Char? = iterator.next() as? Char?

    override fun charOrNull(charset: CharacterSet): Char? = iterator.next() as? Char?

    override fun chars(maxLength: Int, range: CharRange): CharArray = iterator.next() as CharArray

    override fun chars(maxLength: Int, charset: Charset): CharArray = iterator.next() as CharArray

    override fun chars(maxLength: Int, charset: CharacterSet): CharArray = iterator.next() as CharArray

    override fun charsOrNull(maxLength: Int, range: CharRange): CharArray? = iterator.next() as? CharArray?

    override fun charsOrNull(maxLength: Int, charset: Charset): CharArray? =
        iterator.next() as? CharArray?

    override fun charsOrNull(maxLength: Int, charset: CharacterSet): CharArray? =
        iterator.next() as? CharArray?

    override fun double(range: DoubleRange): Double = iterator.next() as Double

    override fun doubleOrNull(range: DoubleRange): Double? = iterator.next() as? Double?

    override fun doubles(maxLength: Int, range: DoubleRange): DoubleArray =
        iterator.next() as DoubleArray

    override fun doublesOrNull(maxLength: Int, range: DoubleRange): DoubleArray? =
        iterator.next() as? DoubleArray?

    override fun float(range: FloatRange): Float = iterator.next() as Float

    override fun floatOrNull(range: FloatRange): Float? = iterator.next() as? Float?

    override fun floats(maxLength: Int, range: FloatRange): FloatArray =
        iterator.next() as FloatArray

    override fun floatsOrNull(maxLength: Int, range: FloatRange): FloatArray? =
        iterator.next() as? FloatArray?

    override fun int(range: IntRange): Int = iterator.next() as Int

    override fun intOrNull(range: IntRange): Int? = iterator.next() as? Int?

    override fun ints(maxLength: Int, range: IntRange): IntArray = iterator.next() as IntArray

    override fun intsOrNull(maxLength: Int, range: IntRange): IntArray? = iterator.next() as? IntArray?

    override fun long(range: LongRange): Long = iterator.next() as Long

    override fun longOrNull(range: LongRange): Long? = iterator.next() as? Long?

    override fun longs(maxLength: Int, range: LongRange): LongArray = iterator.next() as LongArray

    override fun longsOrNull(maxLength: Int, range: LongRange): LongArray? = iterator.next() as? LongArray?

    override fun remainingAsByteArray(): ByteArray = iterator.next() as ByteArray

    override fun remainingAsString(charset: Charset): String = iterator.next() as String

    override fun remainingAsString(charset: CharacterSet): String = iterator.next() as String

    override fun short(range: IntRange): Short = iterator.next() as Short

    override fun shortOrNull(range: IntRange): Short? = iterator.next() as? Short?

    override fun shorts(maxLength: Int, range: IntRange): ShortArray = iterator.next() as ShortArray

    override fun shortsOrNull(maxLength: Int, range: IntRange): ShortArray? = iterator.next() as? ShortArray?

    override fun string(maxLength: Int, charset: Charset): String = iterator.next() as String

    override fun string(maxLength: Int, charset: CharacterSet): String = iterator.next() as String

    override fun string(regex: Regex, configuration: KFuzzer.RegexConfiguration): String =
        iterator.next() as String

    override fun stringOrNull(maxLength: Int, charset: Charset): String? = iterator.next() as? String?

    override fun stringOrNull(maxLength: Int, charset: CharacterSet): String? = iterator.next() as? String?

    override fun stringOrNull(regex: Regex, configuration: KFuzzer.RegexConfiguration): String? =
        iterator.next() as? String?
}
