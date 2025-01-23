package kotlinx.fuzz.test

import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer

object SampleTarget {

    @KFuzzTest
    fun test(data: KFuzzer) {
        if (data.consumeInt() % 2 == 0) {
            if (data.consumeInt() % 3 == 2) {
                if (data.consumeInt() % 31 == 11) {
                    data.consumeBoolean()
                }
            }
        }
    }
}
