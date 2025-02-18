package kotlinx.fuzz

import kotlinx.fuzz.config.KFConfig
import kotlinx.fuzz.config.KFConfigBuilder
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

class KFConfigTest {

    @Test
    fun testBasic() {
        val props = mapOf(
            "kotlinx.fuzz.workDir" to "tmp"
        )
        val config = KFConfigBuilder(props)
            .editFallback {
                target.maxTime = Duration.parse("10s")
            }
            .build()
        assertEquals("tmp", config.global.workDir.fileName.toString())
        assertEquals(Duration.parse("10s"), config.target.maxTime)
    }

    @Test
    fun testEditAndCopy() {
        val props = mapOf(
            "kotlinx.fuzz.workDir" to "tmp",
            "kotlinx.fuzz.maxTime" to "10s",
        )
        val config = KFConfigBuilder(props)
            .editFallback { global.workDir = Path.of("bad") }
            .build()
        val configClone = KFConfig.fromAnotherConfig(config)
            .editOverride { target.maxTime = Duration.parse("5s") }
            .build()
        assertEquals("tmp", configClone.global.workDir.fileName.toString())
        assertEquals(Duration.parse("10s"), config.target.maxTime)
        assertEquals(Duration.parse("5s"), configClone.target.maxTime)
    }

}
