package kotlinx.fuzz

import java.nio.charset.Charset

interface KFuzzEngine {
    fun nextBoolean(probability: Double): Boolean
    fun nextByte(): Byte
    fun nextShort(): Short
    fun nextInt(): Int
    fun nextLong(): Long
    fun nextFloat(): Float
    fun nextDouble(includeSpecialValues: Boolean): Double
    fun nextChar(): Char
    fun nextString(maxLength: Int, charset: Charset): String
    fun nextAsciiString(maxLength: Int): String
    fun nextLetterString(maxLength: Int): String
}