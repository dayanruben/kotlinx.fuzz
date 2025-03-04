package kotlinx.fuzz.test

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer

class JazzerTargetTest {
    @FuzzTest
    fun `yay jazzer test`(data: FuzzedDataProvider) {
        if (data.consumeBoolean()) return
        println(data.consumeRemainingAsBytes())
    }

    @FuzzTest
    fun `with array`(data: ByteArray) {
        if (data.isNotEmpty() && data[0] == 2.toByte()){
            System.getProperty("test")
        }
    }

    @FuzzTest
    fun `autofuzz test to be ignored`(fuzzer: KFuzzer) {}

    @KFuzzTest
    fun `normal test`(fuzzer: KFuzzer) {
       RealUserCode.method1(fuzzer.int(), fuzzer.int(), fuzzer.int(), fuzzer.boolean())
    }
}