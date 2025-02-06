package kotlinx.fuzz.test

import kotlinx.collections.immutable.persistentListOf
import kotlinx.fuzz.IgnoreFailures
import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import kotlinx.fuzz.test.RealUserCode.method1

class AnotherTarget {
    @KFuzzTest
    fun test(data: KFuzzer) {
        persistentListOf(1, 2, 3)
        method1(data.int(), data.int(), data.int(), data.boolean())
    }

    @KFuzzTest
    @IgnoreFailures
    fun `test which fails`(data: KFuzzer) {
        if (data.boolean()) {
            error("Expected failure")
        }
    }

    @KFuzzTest
    fun `test with two fails`(data: KFuzzer) {
        if (data.boolean()) {
            if (data.boolean()) error("Expected failure")
            else throw NullPointerException()
        }
    }
}
