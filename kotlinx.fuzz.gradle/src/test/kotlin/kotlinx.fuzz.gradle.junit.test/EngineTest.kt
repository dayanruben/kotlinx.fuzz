package kotlinx.fuzz.gradle.junit.test

import kotlin.time.Duration.Companion.seconds
import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import kotlinx.fuzz.RunMode
import kotlinx.fuzz.gradle.KFuzzConfigBuilder
import kotlinx.fuzz.gradle.junit.KotlinxFuzzJunitEngine
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.testkit.engine.EngineTestKit

object EngineTest {
    object SimpleFuzzTest {
        @KFuzzTest
        fun `failure test`(data: KFuzzer) {
            if (data.boolean()) {
                error("Expected failure")
            }
        }

        @KFuzzTest
        fun `success test`(@Suppress("UNUSED_PARAMETER", "unused") data: KFuzzer) {
        }
    }

    @BeforeEach
    fun setup() {
        writeToSystemProperties {
            runModes = setOf(RunMode.FUZZING)
            maxSingleTargetFuzzTime = 10.seconds
            instrument = listOf("kotlinx.fuzz.test.**")
            workDir = kotlin.io.path.createTempDirectory("fuzz-test")
            reproducerPath = workDir.resolve("reproducers")
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

private fun writeToSystemProperties(block: KFuzzConfigBuilder.() -> Unit) {
    KFuzzConfigBuilder.build(block).toPropertiesMap()
        .forEach { (key, value) -> System.setProperty(key, value) }
}
