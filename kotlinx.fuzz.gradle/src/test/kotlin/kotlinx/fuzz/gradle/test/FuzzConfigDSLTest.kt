package kotlinx.fuzz.gradle.test

import kotlinx.fuzz.config.KFConfigBuilder
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
        }
        val actualConfig = dsl.build()
        val expectedConfig = KFConfigBuilder(emptyMap()).editOverride {
            global.workDir = Path(".")
        }.build()
        assertEquals(expectedConfig.global.workDir, actualConfig.global.workDir)
    }

}
