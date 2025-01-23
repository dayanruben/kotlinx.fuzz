package kotlinx.fuzz

import com.github.curiousoddman.rgxgen.RgxGen
import java.math.BigDecimal
import java.math.MathContext
import java.nio.charset.Charset
import java.util.*
import kotlinx.fuzz.KFuzzer.RegexConfiguration

class KFuzzerImpl(data: ByteArray) : KFuzzer, Random() {
    private val iterator = Reader(data)

    private operator fun IntRange.contains(other: IntRange): Boolean =
        other.first >= this.first && other.last <= this.last

    /**
     * Fits a number into a range. Always returns the distance from the number to T.MIN_VALUE modulo size of the range
     *
     * @param n a number
     * @param range target range
     * @return a number that fits into the given range
     */
    private inline fun <reified T : Number> fitIntoRange(n: T, range: IntRange): T {
        val rangeSize = range.last.toLong() - range.first + 1
        return when (n) {
            is Byte -> ((n.toLong() - Byte.MIN_VALUE) % rangeSize + range.first).toByte() as T
            is Short -> ((n.toLong() - Short.MIN_VALUE) % rangeSize + range.first).toShort() as T
            is Int -> ((n.toLong() - Int.MIN_VALUE) % rangeSize + range.first).toInt() as T
            else -> error("Not integer type")
        }
    }

    /**
     * Fits a number into a range. Always returns the distance from the number to Long.MIN_VALUE modulo size of the range
     *
     * @param n a number
     * @param range target range
     * @return a number that fits into the given range
     */
    private fun fitIntoRange(n: Long, range: LongRange): Long = when {
        range.first < 0 && range.last > 0 && range.last - Long.MAX_VALUE > range.first -> when {
            n < range.first -> n - Long.MIN_VALUE + range.first

            n <= range.last -> {
                val normalized = n + (range.first - Long.MIN_VALUE)
                when {
                    normalized > range.last -> range.first + (normalized - range.last)
                    else -> normalized
                }
            }

            else -> range.first + (range.first - Long.MIN_VALUE) + (n - range.last) - 1L
        }

        else -> {
            val rangeSize = range.last - range.first + 1
            var normalized = n % rangeSize - Long.MIN_VALUE % rangeSize + range.first
            while (n < range.first) {
                normalized += rangeSize
            }
            normalized
        }
    }

    private fun fitIntoRange(
        value: BigDecimal,
        oldMin: BigDecimal,
        oldMax: BigDecimal,
        newMin: BigDecimal,
        newMax: BigDecimal,
    ): BigDecimal {
        val normalized = value.subtract(oldMin, MathContext.DECIMAL128)
            .divide(oldMax.subtract(oldMin, MathContext.DECIMAL128), MathContext.DECIMAL128)

        return newMin.add(
            normalized.multiply(newMax.subtract(newMin, MathContext.DECIMAL128), MathContext.DECIMAL128),
            MathContext.DECIMAL128,
        )
    }

    /**
     * Implementation taken from #Random.next
     */
    override fun next(bits: Int): Int = (consumeInt() ushr (48 - bits)).toInt()

    override fun consumeBoolean(): Boolean = iterator.readBoolean()

    override fun consumeBooleanOrNull(): Boolean? = when {
        iterator.readBoolean() -> iterator.readBoolean()
        else -> null
    }

    override fun consumeBooleans(maxLength: Int): BooleanArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Boolean>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeBoolean())
        }
        return list.toBooleanArray()
    }

    override fun consumeBooleansOrNull(maxLength: Int): BooleanArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeBooleans(maxLength)
            else -> null
        }
    }

    override fun consumeByte(range: IntRange): Byte {
        require(range.isNotEmpty()) { "range is empty" }
        require(range in Byte.MIN_VALUE..Byte.MAX_VALUE) { "range should be a subset of [Byte.MIN_VALUE..Byte.MAX_VALUE] but was $range" }

        return fitIntoRange(iterator.readByte(), range)
    }

    override fun consumeByteOrNull(range: IntRange): Byte? {
        require(range.isNotEmpty()) { "range is empty" }
        require(range in Byte.MIN_VALUE..Byte.MAX_VALUE) { "range should be a subset of [Byte.MIN_VALUE..Byte.MAX_VALUE] but was $range" }

        return when {
            consumeBoolean() -> consumeByte(range)
            else -> null
        }
    }

    override fun consumeBytes(maxLength: Int, range: IntRange): ByteArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Byte>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeByte(range))
        }
        return list.toByteArray()
    }

    override fun consumeBytesOrNull(maxLength: Int, range: IntRange): ByteArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeBytes(maxLength, range)
            else -> null
        }
    }

    override fun consumeRemainingAsByteArray(): ByteArray {
        val list = mutableListOf<Byte>()
        while (!iterator.isInputFinished()) {
            list.add(consumeByte())
        }
        return list.toByteArray()
    }

    override fun consumeShort(range: IntRange): Short {
        require(range.isNotEmpty()) { "range is empty" }
        require(range in Short.MIN_VALUE..Short.MAX_VALUE) { "range should be a subset of [Short.MIN_VALUE..Short.MAX_VALUE] but was $range" }

        return fitIntoRange(iterator.readShort(), range)
    }

    override fun consumeShortOrNull(range: IntRange): Short? {
        require(range.isNotEmpty()) { "range is empty" }
        require(range in Short.MIN_VALUE..Short.MAX_VALUE) { "range should be a subset of [Short.MIN_VALUE..Short.MAX_VALUE] but was $range" }

        return when {
            consumeBoolean() -> consumeShort(range)
            else -> null
        }
    }

    override fun consumeShorts(maxLength: Int, range: IntRange): ShortArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Short>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeShort(range))
        }
        return list.toShortArray()
    }

    override fun consumeShortsOrNull(maxLength: Int, range: IntRange): ShortArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeShorts(maxLength, range)
            else -> null
        }
    }

    override fun consumeInt(range: IntRange): Int {
        require(range.isNotEmpty()) { "range is empty" }

        return fitIntoRange(iterator.readInt(), range)
    }

    override fun consumeIntOrNull(range: IntRange): Int? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            consumeBoolean() -> consumeInt(range)
            else -> null
        }
    }

    override fun consumeInts(maxLength: Int, range: IntRange): IntArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Int>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeInt(range))
        }
        return list.toIntArray()
    }

    override fun consumeIntsOrNull(maxLength: Int, range: IntRange): IntArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeInts(maxLength, range)
            else -> null
        }
    }

    override fun consumeLong(range: LongRange): Long {
        require(range.isNotEmpty()) { "range is empty" }
        return fitIntoRange(iterator.readLong(), range)
    }

    override fun consumeLongOrNull(range: LongRange): Long? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            consumeBoolean() -> consumeLong(range)
            else -> null
        }
    }

    override fun consumeLongs(maxLength: Int, range: LongRange): LongArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Long>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeLong(range))
        }
        return list.toLongArray()
    }

    override fun consumeLongsOrNull(maxLength: Int, range: LongRange): LongArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeLongs(maxLength, range)
            else -> null
        }
    }

    override fun consumeFloat(range: FloatRange): Float {
        require(range.isNotEmpty()) { "range is empty" }

        return fitIntoRange(
            iterator.readFloat().toBigDecimal(),
            Float.MIN_VALUE.toBigDecimal(),
            Float.MAX_VALUE.toBigDecimal(),
            range.start.toBigDecimal(),
            range.endInclusive.toBigDecimal(),
        ).toFloat()
    }

    override fun consumeFloatOrNull(range: FloatRange): Float? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            consumeBoolean() -> consumeFloat(range)
            else -> null
        }
    }

    override fun consumeFloats(maxLength: Int, range: FloatRange): FloatArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Float>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeFloat(range))
        }
        return list.toFloatArray()
    }

    override fun consumeFloatsOrNull(maxLength: Int, range: FloatRange): FloatArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeFloats(maxLength, range)
            else -> null
        }
    }

    override fun consumeDouble(range: DoubleRange): Double {
        require(range.isNotEmpty()) { "range is empty" }

        return fitIntoRange(
            iterator.readDouble().toBigDecimal(),
            Double.MIN_VALUE.toBigDecimal(),
            Double.MAX_VALUE.toBigDecimal(),
            range.start.toBigDecimal(),
            range.endInclusive.toBigDecimal(),
        ).toDouble()
    }

    override fun consumeDoubleOrNull(range: DoubleRange): Double? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            consumeBoolean() -> consumeDouble(range)
            else -> null
        }
    }

    override fun consumeDoubles(maxLength: Int, range: DoubleRange): DoubleArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Double>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeDouble(range))
        }
        return list.toDoubleArray()
    }

    override fun consumeDoublesOrNull(maxLength: Int, range: DoubleRange): DoubleArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeDoubles(maxLength, range)
            else -> null
        }
    }

    override fun consumeChar(range: CharRange): Char {
        require(range.isNotEmpty()) { "range is empty" }

        return (consumeShort((range.first.code + Short.MIN_VALUE)..(range.last.code + Short.MIN_VALUE)) - Short.MIN_VALUE).toChar()
    }

    override fun consumeCharOrNull(range: CharRange): Char? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            consumeBoolean() -> consumeChar(range)
            else -> null
        }
    }

    override fun consumeChar(charset: Charset): Char {
        val bytes = mutableListOf<Byte>()
        var str = ""
        while (str.isEmpty()) {
            bytes.add(consumeByte())
            str = String(bytes.toByteArray(), charset)
        }
        return str.first()
    }

    override fun consumeCharOrNull(charset: Charset): Char? = when {
        consumeBoolean() -> consumeChar(charset)
        else -> null
    }

    override fun consumeChar(charset: CharacterSet): Char {
        val index = consumeInt(0 until charset.size)
        var current = 0
        for (range in charset.ranges) {
            val rangeSize = range.last - range.first + 1
            when {
                current + rangeSize > index -> return range.first + (index - current)
                else -> current += rangeSize
            }
        }
        return charset.symbols.take(index - current + 1).last()
    }

    override fun consumeCharOrNull(charset: CharacterSet): Char? = when {
        consumeBoolean() -> consumeChar(charset)
        else -> null
    }

    override fun consumeChars(maxLength: Int, range: CharRange): CharArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Char>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeChar(range))
        }
        return list.toCharArray()
    }

    override fun consumeCharsOrNull(maxLength: Int, range: CharRange): CharArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeChars(maxLength, range)
            else -> null
        }
    }

    override fun consumeChars(maxLength: Int, charset: Charset): CharArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Char>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeChar(charset))
        }
        return list.toCharArray()
    }

    override fun consumeCharsOrNull(maxLength: Int, charset: Charset): CharArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeChars(maxLength, charset)
            else -> null
        }
    }

    override fun consumeChars(maxLength: Int, charset: CharacterSet): CharArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Char>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeChar(charset))
        }
        return list.toCharArray()
    }

    override fun consumeCharsOrNull(maxLength: Int, charset: CharacterSet): CharArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeChars(maxLength, charset)
            else -> null
        }
    }

    override fun consumeString(maxLength: Int, charset: Charset): String {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val byteBuffer = mutableListOf<Byte>()
        while (true) {
            byteBuffer.add(iterator.readByte())
            if (String(byteBuffer.toByteArray(), charset).length >= maxLength) {
                break
            }
        }

        return String(byteBuffer.toByteArray(), charset).take(maxLength)
    }

    override fun consumeStringOrNull(maxLength: Int, charset: Charset): String? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeString(maxLength, charset)
            else -> null
        }
    }

    override fun consumeRemainingAsString(charset: Charset): String {
        val bytes = mutableListOf<Byte>()
        while (!iterator.isInputFinished()) {
            bytes.add(iterator.readByte())
        }
        return String(bytes.toByteArray(), charset)
    }

    override fun consumeString(maxLength: Int, charset: CharacterSet): String {
        require(maxLength > 0) { "maxLength must be greater than 0" }
        return String(consumeChars(maxLength, charset))
    }

    override fun consumeStringOrNull(maxLength: Int, charset: CharacterSet): String? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeString(maxLength, charset)
            else -> null
        }
    }

    override fun consumeRemainingAsString(charset: CharacterSet): String = buildString {
        while (!iterator.isInputFinished()) {
            append(consumeChar(charset))
        }
    }

    override fun consumeString(regex: Regex, configuration: RegexConfiguration): String {
        val rgxGen = RgxGen.parse(configuration.asRegexProperties(), regex.pattern)
        return rgxGen.generate(this)
    }

    override fun consumeStringOrNull(regex: Regex, configuration: RegexConfiguration) = when {
        consumeBoolean() -> consumeString(regex, configuration)
        else -> null
    }

    private class Reader(data: ByteArray) {
        private val iterator = data.iterator()

        fun isInputFinished(): Boolean = !iterator.hasNext()

        fun readBoolean(): Boolean = readByte() != 0.toByte()

        fun readByte(): Byte = when {
            iterator.hasNext() -> iterator.next()
            else -> 0
        }

        fun readShort(): Short = (readByte().toInt() shl 8 or (readByte().toInt() and 0xFF)).toShort()

        fun readInt(): Int =
            (readByte().toInt() shl 24) or ((readByte().toInt() and 0xFF) shl 16) or
                ((readByte().toInt() and 0xFF) shl 8) or (readByte().toInt() and 0xFF)

        fun readLong(): Long = (readInt().toLong() shl 32) or (readInt().toLong() and 0xFF_FF_FF_FFL)

        fun readFloat(): Float = Float.fromBits(readInt())

        fun readDouble(): Double = Double.fromBits(readLong())
    }
}
