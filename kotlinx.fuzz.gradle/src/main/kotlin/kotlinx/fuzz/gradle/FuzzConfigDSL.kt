package kotlinx.fuzz.gradle

import kotlinx.fuzz.config.KFuzzConfig
import kotlinx.fuzz.config.KFuzzConfigBuilder
import kotlin.reflect.KProperty

abstract class FuzzConfigDSL {

    private val builder by lazy { KFuzzConfig.fromSystemProperties() }

    // ========== global ==========
    var workDir by KFConfigDelegate { global::workDir }
    var reproducerDir by KFConfigDelegate { global::reproducerDir }
    var hooks by KFConfigDelegate { global::hooks }
    var logLevel by KFConfigDelegate { global::logLevel }
    var regressionEnabled by KFConfigDelegate { global::regressionEnabled }

    // ========== target ==========
    var maxFuzzTimePerTarget by KFConfigDelegate { target::maxFuzzTime }
    var keepGoing by KFConfigDelegate { target::keepGoing }
    var instrument by KFConfigDelegate { target::instrument }
    var customHookExcludes by KFConfigDelegate { target::customHookExcludes }
    var dumpCoverage by KFConfigDelegate { target::dumpCoverage }

    // ========== engine ==========

    sealed interface EngineConfigDSL

    inner class JazzerConfigDSL : EngineConfigDSL {
        var libFuzzerRssLimit by KFConfigDelegate { engine::libFuzzerRssLimitMb }
        var enableLogging by KFConfigDelegate { engine::enableLogging }
    }

    /**
     * TODO: no support for different engines yet. See [KFuzzConfigBuilder.KFuzzConfigImpl]
     */
    private val engineDSL = JazzerConfigDSL()

    fun engine(block: JazzerConfigDSL.() -> Unit) {
        engineDSL.block()
    }

    // ========== coverage ==========

    inner class CoverageConfigDSL {
        var reportTypes by KFConfigDelegate { coverage::reportTypes }
        var includeDependencies by KFConfigDelegate { coverage::includeDependencies }
    }

    private val coverageDSL = CoverageConfigDSL()

    fun coverage(block: CoverageConfigDSL.() -> Unit) {
        coverageDSL.block()
    }

    // ========== internals ==========

    private inner class KFConfigDelegate<T : Any>(
        propertySelector: KFuzzConfigBuilder.KFuzzConfigImpl.() -> KProperty<T>
    ) {
        private val kfProp = builder.getPropertyDelegate(propertySelector)

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
            kfProp.getValue(thisRef, property)

        // because editFallback is necessary, getValue() is not possible before building
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            builder.editFallback { kfProp.setValue(thisRef, property, value) }
        }
    }

    fun build(): KFuzzConfig = builder.build()
}
