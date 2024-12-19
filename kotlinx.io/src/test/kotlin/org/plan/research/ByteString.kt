package org.plan.research

//import sun.nio.cs.ext.MacCyrillic
import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.bytestring.*
import org.plan.research.Constants.INIT_BYTES_COUNT
import java.nio.charset.Charset
import kotlin.test.assertEquals

object ByteStringTargets {
    val CHARSETS: Array<Charset> = arrayOf(
        Charsets.UTF_8,

        Charsets.UTF_16,
        Charsets.UTF_16BE,
        Charsets.UTF_16LE,

        Charsets.UTF_32,
        Charsets.UTF_32BE,
        Charsets.UTF_32LE,

//        Charsets.ISO_8859_1,
//        Charsets.US_ASCII,
    )

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun fromStringAndBack(data: FuzzedDataProvider): Unit = with(data) {
        val charset = pickValue(CHARSETS)
        val s = data.consumeRemainingAsString()

        if (s != charset.decode(charset.encode(s)).toString()) return

        val bs = s.encodeToByteString(charset)
        val res = bs.decodeToString(charset)
        assertEquals(s, res)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun compareTo(data: FuzzedDataProvider): Unit = with(data) {
        val maxLength = 5

        val s1 = data.consumeBytes(maxLength).toUByteArray()
        val s2 = data.consumeBytes(maxLength).toUByteArray()

        val cmp = s1.zip(s2).firstOrNull { (a, b) -> a != b }
            ?.let { (a, b) -> a.compareTo(b) } ?: s1.size.compareTo(s2.size)
//        val cmp = Arrays.compare(s1, s2).normalize()

        val bs1 = ByteString(s1.toByteArray())
        val bs2 = ByteString(s2.toByteArray())
        val cmp2 = bs1.compareTo(bs2).normalize()

        assertEquals(cmp, cmp2)
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun toByteBufferAndBack(data: FuzzedDataProvider): Unit = with(data) {
        val b = ByteString(data.consumeBytes(INIT_BYTES_COUNT))
        val bb = b.asReadOnlyByteBuffer().getByteString()
        assertEquals(b, bb)
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun builder(data: FuzzedDataProvider): Unit = with(data) {
        val builder = ByteStringBuilder()
        val sb = StringBuilder()
        val opsN = consumeInt(0, Constants.MAX_OPERATIONS_NUMBER)
        repeat(opsN) {
            val s = data.consumeString(10)
            builder.append(s.encodeToByteString())
            sb.append(s)
        }
        assertEquals(sb.length, builder.toByteString().decodeToString().length)
    }

    fun Int.normalize() = when {
        this == 0 -> 0
        this < 0 -> -1
        else -> 1
    }
}