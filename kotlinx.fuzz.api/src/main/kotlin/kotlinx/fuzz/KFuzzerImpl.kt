package kotlinx.fuzz

import java.nio.charset.Charset

class KFuzzerImpl(data: ByteArray) : KFuzzer {
    private class Reader(data: ByteArray) {
        private val iterator = data.iterator()

        fun isInputFinished() = !iterator.hasNext()

        fun readBoolean() = readByte() != 0.toByte()

        fun readByte(throws: Boolean = false) = if (iterator.hasNext()) {
            iterator.next()
        } else {
            if (throws) {
                throw RuntimeException("Cannot read byte from array")
            } else {
                0
            }
        }

        fun readShort() = try {
            (readByte(true).toInt() shl 8 or (readByte(true).toInt() and 0xFF)).toShort()
        } catch (e: RuntimeException) {
            0
        }

        fun readInt() = try {
            (readByte(true).toInt() shl 24) or ((readByte(true).toInt() and 0xFF) shl 16) or
                    ((readByte(true).toInt() and 0xFF) shl 8) or (readByte(true).toInt() and 0xFF)
        } catch (e: RuntimeException) {
            0
        }

        fun readLong() = (readInt().toLong() shl 32) or (readInt().toLong() and 0xFFFFFFFFL)

        fun readFloat() = Float.fromBits(readInt())

        fun readDouble() = Double.fromBits(readLong())

        fun readChar() = readShort().toInt().toChar()
    }

    private val iterator = Reader(data)

    override fun consumeBoolean() = iterator.readBoolean()

    override fun consumeBooleanOrNull() = if (iterator.readBoolean()) {
        iterator.readBoolean()
    } else {
        null
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

        if (consumeBoolean()) return null
        return consumeBooleans(maxLength)
    }

    override fun consumeByte(range: IntRange): Byte {
        require(!range.isEmpty()) { "range is empty" }
        require(range.first >= Byte.MIN_VALUE && range.last <= Byte.MAX_VALUE) { "range should be a subset of [Byte.MIN_VALUE] but was [Byte.MAX_VALUE]" }

        val normalized =
            (iterator.readByte().toDouble() - Byte.MIN_VALUE) / (Byte.MAX_VALUE.toDouble() - Byte.MIN_VALUE)
        return (range.first + (normalized * (range.last.toDouble() - range.first))).toInt().toByte()
    }

    override fun consumeByteOrNull(range: IntRange): Byte? {
        require(!range.isEmpty()) { "range is empty" }
        require(range.first >= Byte.MIN_VALUE && range.last <= Byte.MAX_VALUE) { "range should be a subset of [Byte.MIN_VALUE] but was [Byte.MAX_VALUE]" }

        if (iterator.readBoolean()) return null
        return consumeByte(range)
    }

    override fun consumeBytes(maxLength: Int, range: IntRange): ByteArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Byte>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeByte())
        }
        return list.toByteArray()
    }

    override fun consumeBytesOrNull(maxLength: Int, range: IntRange): ByteArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        if (consumeBoolean()) return null
        return consumeBytes(maxLength, range)
    }

    override fun consumeRemainingAsByteArray(): ByteArray {
        val list = mutableListOf<Byte>()
        while (!iterator.isInputFinished()) {
            list.add(consumeByte())
        }
        return list.toByteArray()
    }

    override fun consumeShort(range: IntRange): Short {
        require(!range.isEmpty()) { "range is empty" }
        require(range.first >= Short.MIN_VALUE && range.last <= Short.MAX_VALUE) { "range should be a subset of [Short.MIN_VALUE] but was [Short.MAX_VALUE]" }

        val normalized =
            (iterator.readShort().toDouble() - Short.MIN_VALUE) / (Short.MAX_VALUE.toDouble() - Short.MIN_VALUE)
        return (range.first + (normalized * (range.last.toDouble() - range.first))).toInt().toShort()
    }

    override fun consumeShortOrNull(range: IntRange): Short? {
        require(!range.isEmpty()) { "range is empty" }
        require(range.first >= Short.MIN_VALUE && range.last <= Short.MAX_VALUE) { "range should be a subset of [Short.MIN_VALUE] but was [Short.MAX_VALUE]" }

        if (iterator.readBoolean()) return null
        return consumeShort(range)
    }

    override fun consumeShorts(maxLength: Int, range: IntRange): ShortArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Short>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeShort())
        }
        return list.toShortArray()
    }

    override fun consumeShortsOrNull(maxLength: Int, range: IntRange): ShortArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        if (consumeBoolean()) return null
        return consumeShorts(maxLength, range)
    }

    override fun consumeInt(range: IntRange): Int {
        require(!range.isEmpty()) { "range is empty" }

        val normalized =
            (iterator.readInt().toDouble() - Int.MIN_VALUE) / (Int.MAX_VALUE.toDouble() - Int.MIN_VALUE)
        return (range.first + (normalized * (range.last.toDouble() - range.first))).toInt()
    }

    override fun consumeIntOrNull(range: IntRange): Int? {
        require(!range.isEmpty()) { "range is empty" }

        if (iterator.readBoolean()) return null
        return consumeInt(range)
    }

    override fun consumeInts(maxLength: Int, range: IntRange): IntArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Int>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeInt())
        }
        return list.toIntArray()
    }

    override fun consumeIntsOrNull(maxLength: Int, range: IntRange): IntArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        if (consumeBoolean()) return null
        return consumeInts(maxLength, range)
    }

    override fun consumeLong(range: LongRange): Long {
        require(!range.isEmpty()) { "range is empty" }

        val normalized =
            (iterator.readLong().toDouble() - Long.MIN_VALUE) / (Long.MAX_VALUE.toDouble() - Long.MIN_VALUE)
        return (range.first + (normalized * (range.last.toDouble() - range.first))).toLong()
    }

    override fun consumeLongOrNull(range: LongRange): Long? {
        require(!range.isEmpty()) { "range is empty" }

        if (iterator.readBoolean()) return null
        return consumeLong(range)
    }

    override fun consumeLongs(maxLength: Int, range: LongRange): LongArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Long>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeLong())
        }
        return list.toLongArray()
    }

    override fun consumeLongsOrNull(maxLength: Int, range: LongRange): LongArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        if (consumeBoolean()) return null
        return consumeLongs(maxLength, range)
    }

    override fun consumeFloat(range: FloatRange): Float {
        require(!range.isEmpty()) { "range is empty" }

        val normalized =
            (iterator.readFloat().toDouble() - Float.MIN_VALUE) / (Float.MAX_VALUE.toDouble() - Float.MIN_VALUE)
        return (range.start + (normalized * (range.endInclusive.toDouble() - range.start))).toFloat()
    }

    override fun consumeFloatOrNull(range: FloatRange): Float? {
        require(!range.isEmpty()) { "range is empty" }

        if (iterator.readBoolean()) return null
        return consumeFloat(range)
    }

    override fun consumeFloats(maxLength: Int, range: FloatRange): FloatArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Float>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeFloat())
        }
        return list.toFloatArray()
    }

    override fun consumeFloatsOrNull(maxLength: Int, range: FloatRange): FloatArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        if (consumeBoolean()) return null
        return consumeFloats(maxLength, range)
    }

    override fun consumeDouble(range: DoubleRange): Double {
        require(!range.isEmpty()) { "range is empty" }

        val normalized = (iterator.readDouble() - Double.MIN_VALUE) / (Double.MAX_VALUE - Double.MIN_VALUE)
        return (range.start + (normalized * (range.endInclusive - range.start)))
    }

    override fun consumeDoubleOrNull(range: DoubleRange): Double? {
        require(!range.isEmpty()) { "range is empty" }

        if (iterator.readBoolean()) return null
        return consumeDouble(range)
    }

    override fun consumeDoubles(maxLength: Int, range: DoubleRange): DoubleArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Double>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeDouble())
        }
        return list.toDoubleArray()
    }

    override fun consumeDoublesOrNull(maxLength: Int, range: DoubleRange): DoubleArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        if (consumeBoolean()) return null
        return consumeDoubles(maxLength, range)
    }

    override fun consumeChar(range: CharRange): Char {
        require(!range.isEmpty()) { "range is empty" }

        return range.first + (iterator.readChar() - range.first)
    }

    override fun consumeCharOrNull(range: CharRange): Char? {
        require(!range.isEmpty()) { "range is empty" }

        if (iterator.readBoolean()) return null
        return consumeChar(range)
    }

    override fun consumeChars(maxLength: Int, range: CharRange): CharArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Char>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeChar())
        }
        return list.toCharArray()
    }

    override fun consumeCharsOrNull(maxLength: Int, range: CharRange): CharArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        if (consumeBoolean()) return null
        return consumeChars(maxLength, range)
    }

    override fun consumeString(maxLength: Int, charset: Charset): String {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val length = consumeInt(0..maxLength)
        val bytes = mutableListOf<Byte>()
        while (bytes.size < length) {
            bytes.add(iterator.readByte())
        }
        return String(bytes.toByteArray(), charset)
    }

    override fun consumeStringOrNull(maxLength: Int, charset: Charset): String? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        if (consumeBoolean()) return null
        return consumeString(maxLength, charset)
    }

    override fun consumeRemainingAsString(charset: Charset): String {
        val bytes = mutableListOf<Byte>()
        while (!iterator.isInputFinished()) {
            bytes.add(iterator.readByte())
        }
        return String(bytes.toByteArray(), charset)
    }

    override fun consumeAsciiString(maxLength: Int) = consumeString(maxLength, Charsets.US_ASCII)

    override fun consumeAsciiStringOrNull(maxLength: Int) = consumeStringOrNull(maxLength, Charsets.US_ASCII)

    override fun consumeRemainingAsAsciiString() = consumeRemainingAsString(Charsets.US_ASCII)

    override fun consumeLetterString(maxLength: Int): String {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val length = consumeInt(0..maxLength)
        var result = ""
        while (result.length < length) {
            val index = consumeByte(0 until 52)
            result += if (index < 26) {
                'a' + index.toInt()
            } else {
                'A' + index.toInt() - 26
            }
        }
        return result
    }

    override fun consumeLetterStringOrNull(maxLength: Int): String? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        if (consumeBoolean()) return null
        return consumeLetterString(maxLength)
    }

    override fun consumeRemainingAsLetterString(): String {
        var result = ""
        while (!iterator.isInputFinished()) {
            val index = consumeByte(0 until 52)
            result += if (index < 26) {
                'a' + index.toInt()
            } else {
                'A' + index.toInt() - 26
            }
        }
        return result
    }

}