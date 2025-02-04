package kotlinx.fuzz.test

import kotlinx.fuzz.IgnoreFailures
import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer

class AnotherTarget {
    @KFuzzTest
    fun test(data: KFuzzer) {
        if (data.int() % 2 == 0) {
            if (data.int() % 3 == 2) {
                if (data.int() % 31 == 11) {
                    if(data.boolean()) throw NullPointerException()
                    else throw AssertionError()
                }
            }
        }
    }

    @KFuzzTest
    @IgnoreFailures
    fun `test which fails`(data: KFuzzer) {
        if (data.boolean()) {
            error("Expected failure")
        }
    }
}
