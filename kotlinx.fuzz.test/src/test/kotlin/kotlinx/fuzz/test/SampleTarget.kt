package kotlinx.fuzz.test

import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer

object SampleTarget {

    @KFuzzTest
    fun test(data: KFuzzer) {
        RealUserCode.method1(data.int(), data.int(), data.int(), data.boolean())
    }
}
