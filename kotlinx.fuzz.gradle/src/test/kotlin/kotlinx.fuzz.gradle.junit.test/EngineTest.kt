package kotlinx.fuzz.gradle.junit.test

import kotlinx.fuzz.IgnoreFailures
import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import kotlinx.fuzz.config.KFuzzConfigBuilder
import kotlinx.fuzz.gradle.junit.KotlinxFuzzJunitEngine
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.testkit.engine.EngineTestKit
import kotlin.time.Duration.Companion.seconds

object EngineTest {
    object SimpleFuzzTest {
        @KFuzzTest
        fun `failure test`(data: KFuzzer) {
            if (data.boolean()) {
                error("Expected failure")
            }
        }

        @IgnoreFailures
        @KFuzzTest
        fun `ignored failure test`(data: KFuzzer) {
            if (data.boolean()) {
                error("Expected failure")
            }
        }

        @KFuzzTest
        fun `success test`(@Suppress("UNUSED_PARAMETER", "unused") data: KFuzzer) {
        }

        @KFuzzTest
        fun `two failure test`(data: KFuzzer) {
            if (data.boolean()) {
                if (data.boolean()) {
                    error("Expected failure 1")
                } else {
                    throw RuntimeException("Expected failure 2")
                }
            }
        }
    }

    @BeforeEach
    fun setup() {
       writeToSystemProperties {
            // subtle check that getValue() works before build()
            global.workDir = kotlin.io.path.createTempDirectory("fuzz-test")
            global.reproducerDir = global.workDir.resolve("reproducers")
            target.maxFuzzTime = 5.seconds
            target.instrument = listOf("kotlinx.fuzz.test.**")
            target.keepGoing = 2
        }
    }

    @Test
    fun `one pass one fail`() {
        val successTests = 3L
        val failedTests = 1L
        val startedTests = successTests + failedTests

        EngineTestKit
            .engine(KotlinxFuzzJunitEngine())
            .selectors(selectClass(SimpleFuzzTest::class.java))
            .execute()
            .testEvents()
            .assertStatistics {
                it.started(startedTests).succeeded(successTests).failed(failedTests)
            }
    }
}

fun writeToSystemProperties(config: KFuzzConfigBuilder.KFuzzConfigImpl.() -> Unit) {
    KFuzzConfigBuilder(emptyMap())
        .editOverride(config)
        .build()
        .toPropertiesMap()
        .forEach { (key, value) -> System.setProperty(key, value) }
}
