package kotlinx.fuzz.junit.test

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.time.Duration.Companion.seconds
import kotlinx.fuzz.IgnoreFailures
import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import kotlinx.fuzz.config.KFuzzConfigBuilder
import kotlinx.fuzz.junit.KotlinxFuzzJunitEngine
import org.junit.jupiter.api.AfterAll
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

    object JazzerTestContainer {
        @FuzzTest
        @Suppress("BACKTICKS_PROHIBITED")
        fun `jazzer test`(data: FuzzedDataProvider) {
            if (data.consumeBoolean()) {
                System.getProperty("aaa")
            }
        }

        @FuzzTest
        @Suppress("BACKTICKS_PROHIBITED")
        fun `jazzer test array`(data: ByteArray) {
            if (data.isNotEmpty() && data[0] == 0.toByte()) {
                System.getProperty("aaa")
            }
        }
    }

    @BeforeEach
    fun setup() {
        writeToSystemProperties {
            // subtle check that getValue() works before build()
            with(global) {
                workDir = createTempDirectory("fuzz-test")
                reproducerDir = global.workDir.resolve("reproducers")
                instrument = listOf("kotlinx.fuzz.test.**")
                supportJazzerTargets = true
            }
            with(target) {
                maxFuzzTime = 5.seconds
                keepGoing = 2
            }
        }
    }

    @Test
    fun `passes and failures`() {
        val successTests = 2L
        val failedTests = 5L
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

    // without cleanup, KFuzzConfigTest fails hehe
    @AfterAll
    @JvmStatic
    fun cleanup() = cleanupSystemProperties()

    @Test
    fun `jazzer api support`() {
        val successTests = 2L
        val failedTests = 0L
        val startedTests = successTests + failedTests

        EngineTestKit
            .engine(KotlinxFuzzJunitEngine())
            .selectors(selectClass(JazzerTestContainer::class.java))
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

fun cleanupSystemProperties() {
    val needsCleanup = listOf(
        "kotlinx.fuzz.workDir",
        "kotlinx.fuzz.reproducerDir",
        "kotlinx.fuzz.instrument",
    )
    needsCleanup.forEach { prop -> System.clearProperty(prop) }
}
