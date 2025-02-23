package kotlinx.fuzz.gradle.test

import kotlinx.fuzz.config.KFuzzConfigBuilder
import kotlinx.fuzz.gradle.FuzzConfigDSL
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

object FuzzConfigDSLTest {

    @Test
    fun test() {
        val dsl = object : FuzzConfigDSL() {}
        dsl.apply {
            workDir = Path(".")
            reproducerDir = Path(".")
            instrument = emptyList()
        }
        val actualConfig = dsl.build()
        val expectedConfig = KFuzzConfigBuilder(emptyMap()).editOverride {
            global.workDir = Path(".")
            global.reproducerDir = Path(".")
            target.instrument = emptyList()
        }.build()
        assertEquals(expectedConfig.global.workDir, actualConfig.global.workDir)
    }

}
