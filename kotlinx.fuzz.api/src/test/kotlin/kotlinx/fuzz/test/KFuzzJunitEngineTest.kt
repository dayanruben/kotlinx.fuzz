package kotlinx.fuzz.test

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.fuzz.KFuzzTest

object KFuzzJunitEngineTest {
    @KFuzzTest
    fun not42(data: FuzzedDataProvider) {
        if (data.consumeInt() == 42) {
            error("error 42!")
        }
    }

//    @KFuzzTest
//    fun `aaa test`(data: FuzzedDataProvider) {
//    }
}