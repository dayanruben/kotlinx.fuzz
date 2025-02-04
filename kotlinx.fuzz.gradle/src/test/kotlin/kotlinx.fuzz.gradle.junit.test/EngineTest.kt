package kotlinx.fuzz.gradle.junit.test

import java.io.File
import kotlin.time.Duration.Companion.seconds
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
        fun `failure test`(data: KFuzzer) {
            if (data.boolean()) {
                error("Expected failure")
            }
        }

        @KFuzzTest
        fun `success test`(@Suppress("UNUSED_PARAMETER", "unused") data: KFuzzer) {
        }

        @KFuzzTest
        fun `failure test2`(data: KFuzzer) {
            if (data.int() in 16..24) {
                val file = File("fuzz-test.txt")
                file.readText()
            }
        }

        @KFuzzTest
        fun `failure test3`(data: KFuzzer) {
            if (data.int() in 5..15) {
                val b = System.getProperty("abibaboba")
                System.setProperty("abacaba", b!!)
            }
        }

        @KFuzzTest
        fun `failure test4`(data: KFuzzer) {
            if (data.int() in 25..215) {
                throw IllegalArgumentException("Expected failure")
            }
        }
    }

    @BeforeEach
    fun setup() {
        writeToSystemProperties {
            maxSingleTargetFuzzTime = 10.seconds
            instrument = listOf("kotlinx.fuzz.test.**")
            workDir = kotlin.io.path.createTempDirectory("fuzz-test")
        }
    }

    @Test
    fun `one pass four fail`() {
        EngineTestKit
            .engine(KotlinxFuzzJunitEngine())
            .selectors(selectClass(SimpleFuzzTest::class.java))
            .execute()
            .testEvents()
            .assertStatistics { it.started(5).succeeded(1).failed(4) }
    }
}

private fun writeToSystemProperties(block: KFuzzConfigBuilder.() -> Unit) {
    KFuzzConfigBuilder.build(block).toPropertiesMap()
        .forEach { (key, value) -> System.setProperty(key, value) }
}
