package kotlinx.fuzz.gradle.junit.test

import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlinx.fuzz.IgnoreFailures
import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import kotlinx.fuzz.gradle.KFuzzConfigBuilder
import kotlinx.fuzz.gradle.junit.KotlinxFuzzJunitEngine
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.testkit.engine.EngineTestKit

object EngineTest {
    object SimpleFuzzTest {
        @KFuzzTest
        fun `one failure test`(data: KFuzzer) {
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
        fun `failure test2`(data: KFuzzer) {
            if (data.int() in 21..25) {
                val file = File("fuzz-test.txt")
                file.readText()
            } else if (data.int() in 26..30) {
                throw IllegalArgumentException("Expected failure")
            }
        }

        @KFuzzTest
        fun `failure test3`(data: KFuzzer) {
            if (data.int() in 5..15) {
                val b = System.getProperty("abibaboba")
                System.setProperty("abacaba", b!!)
            } else if (data.int() in 16..20) {
                val file = File("fuzz-test.txt")
                file.readText()
            }
        }

        @KFuzzTest
        fun `failure test4`(data: KFuzzer) {
            if (data.int() in 30..215) {
                throw IllegalArgumentException("Expected failure")
            } else if (data.int() in 216..314) {
                val b = System.getProperty("abibaboba")
                System.setProperty("abacaba", b!!)
            }
        }
    }

    @BeforeEach
    fun setup() {
        writeToSystemProperties {
            maxSingleTargetFuzzTime = 5.seconds
            instrument = listOf("kotlinx.fuzz.test.**")
            workDir = kotlin.io.path.createTempDirectory("fuzz-test")
            keepGoing = 2
        }
    }

    @Test
    fun `three pass three fail`() {
        val successTests = 3L
        val failedTests = 3L
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

private fun writeToSystemProperties(block: KFuzzConfigBuilder.() -> Unit) {
    KFuzzConfigBuilder.build(block).toPropertiesMap()
        .forEach { (key, value) -> System.setProperty(key, value) }
}
