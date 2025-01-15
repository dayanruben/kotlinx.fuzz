package kotlinx.fuzz.gradle.test

import kotlinx.fuzz.gradle.FuzzConfigBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

object FuzzConfigBuilderTest {
    @Test
    fun `not initialize something`() {
        assertThrows<Throwable> { FuzzConfigBuilder.build {} }
    }

    @Test
    fun `all set`(){
        assertDoesNotThrow {
            FuzzConfigBuilder.build {
                fuzzEngine = "asdf"
                instrument = listOf("1", "2")
                maxSingleTargetFuzzTime = 30
            }
        }
    }

}