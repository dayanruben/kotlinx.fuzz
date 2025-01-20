package kotlinx.fuzz.gradle.junit.test

import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import kotlinx.fuzz.gradle.KFuzzConfigBuilder
import kotlinx.fuzz.gradle.junit.KotlinxFuzzJunitEngine
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.testkit.engine.EngineTestKit
import kotlin.time.Duration.Companion.seconds

object EngineTest {
    @BeforeEach
    fun setup() {
        KFuzzConfigBuilder.writeToSystemProperties {
            maxSingleTargetFuzzTime = 10.seconds
            instrument = listOf("kotlinx.fuzz.test.**")
        }
    }

    object SimpleFuzzTest {
        @KFuzzTest
        fun `failure test`(data: KFuzzer) {
            if (data.consumeBoolean()) {
                error("Expected failure")
            }
        }

        @KFuzzTest
        fun `success test`(@Suppress("UNUSED_PARAMETER") data: KFuzzer) {
        }
    }

    @Test
    fun `one pass one fail`() {
        EngineTestKit
            .engine(KotlinxFuzzJunitEngine())
            .selectors(selectClass(SimpleFuzzTest::class.java))
            .execute()
            .testEvents()
            .assertStatistics { it.started(2).succeeded(1).failed(1) }
    }
}
