package kotlinx.fuzz.gradle.junit.test

import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import kotlinx.fuzz.gradle.junit.KotlinxFuzzJunitEngine
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.testkit.engine.EngineTestKit
import java.io.File

object EngineTest {
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

        @KFuzzTest
        fun `failure test2`(data: KFuzzer) {
            if (data.consumeInt() in 16..24) {
                val file = File("fuzz-test.txt")
                file.readText()
            }
        }

        @KFuzzTest
        fun `failure test3`(data: KFuzzer) {
            if (data.consumeInt() in 5..15) {
                val b = System.getProperty("abibaboba")
                println(b!!.length)
            }
        }

        @KFuzzTest
        fun `failure test4`(data: KFuzzer) {
            if (data.consumeInt() in 25..215) {
                throw IllegalArgumentException("Expected failure")
            }
        }
    }

    @Test
    fun `one pass one fail`() {
        EngineTestKit
            .engine(KotlinxFuzzJunitEngine())
            .selectors(selectClass(SimpleFuzzTest::class.java))
            .execute()
            .testEvents()
            .assertStatistics { it.started(5).succeeded(1).failed(4) }
    }
}
