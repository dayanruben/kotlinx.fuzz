package kotlinx.fuzz.test

import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer

class BadTarget {

    // should fail in current process
    @KFuzzTest(maxFuzzTime = "not even a duration")
    fun notDuration(data: KFuzzer) {
        data.boolean()
    }

    // should fail in new process
    // TODO: fails with wrong error! (NoSuchFile instead of validation exception)
    @KFuzzTest(maxFuzzTime = "-1s")
    fun negativeDuration(data: KFuzzer) {
        data.boolean()
    }
}
