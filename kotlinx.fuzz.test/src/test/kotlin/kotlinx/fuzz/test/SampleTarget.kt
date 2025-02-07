package kotlinx.fuzz.test

import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer

object SampleTarget {

    @KFuzzTest
    fun test(data: KFuzzer) {
        RealUserCode.method1(data.int(), data.int(), data.int(), data.boolean())
    }

    @KFuzzTest(keepGoing = 2, maxFuzzTime = "5s")
    fun overriddenConfig(data: KFuzzer) {
        // A _hacky_ way to test if config params are actually overridden.
        // It's based on probability of fuzzer finding the second exception before first being basically 0.
        // If keepGoing=2 is not set, we will only get the first error.
        if (data.boolean()) {
            error("first error")
        }
        if (data.int() * data.int() == 25) {
            error("second error")
        }
    }
}
