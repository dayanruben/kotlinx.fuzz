package kotlinx.fuzz.test

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.junit.KFuzzJunitEngine
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.testkit.engine.EngineTestKit

object EngineTest {
    @Test
    fun test() {
        EngineTestKit
            .engine(KFuzzJunitEngine())
            .selectors(selectClass(SimpleFuzzTest::class.java))
            .execute()
            .testEvents()
            .assertStatistics { it.started(2).succeeded(1).failed(1) }
    }

    object SimpleFuzzTest {
        @KFuzzTest
        fun `failure test`(data: FuzzedDataProvider) {
            if (data.consumeBoolean()) {
                error("Expected failure")
            }
        }

        @KFuzzTest
        fun `success test`(data: FuzzedDataProvider) {
        }
    }
}