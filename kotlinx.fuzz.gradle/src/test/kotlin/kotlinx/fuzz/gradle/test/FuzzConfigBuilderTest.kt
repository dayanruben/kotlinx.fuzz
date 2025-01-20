package kotlinx.fuzz.gradle.test

import kotlin.time.Duration.Companion.seconds
import kotlinx.fuzz.gradle.KFuzzConfigBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

object FuzzConfigBuilderTest {
    @Test
    fun `not initialize something`() {
        @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
        assertThrows<Throwable> { KFuzzConfigBuilder.build {} }
    }

    @Test
    fun `0 maxSingleTargetFuzzTime fails`() {
        assertThrows<IllegalArgumentException> {
            KFuzzConfigBuilder.build {
                maxSingleTargetFuzzTime = 0.seconds
                instrument = emptyList()
            }
        }
    }

    @Test
    fun `enough set`() {
        assertDoesNotThrow {
            KFuzzConfigBuilder.build {
                instrument = listOf("1", "2")
                maxSingleTargetFuzzTime = 30.seconds
            }
        }
    }

    @Test
    fun `all set`() {
        assertDoesNotThrow {
            KFuzzConfigBuilder.build {
                fuzzEngine = "engine"
                hooks = true
                keepGoing = 339
                instrument = listOf()
                customHookExcludes = listOf("exclude")
                maxSingleTargetFuzzTime = 1000.seconds
            }
        }
    }
}
