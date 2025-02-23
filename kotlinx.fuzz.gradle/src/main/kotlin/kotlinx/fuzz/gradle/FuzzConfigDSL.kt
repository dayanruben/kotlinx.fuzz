package kotlinx.fuzz.gradle

import kotlinx.fuzz.config.JazzerConfig
import kotlinx.fuzz.config.KFConfig
import kotlinx.fuzz.config.KFConfigBuilder
import org.slf4j.event.Level
import java.nio.file.Path
import kotlin.reflect.KProperty
import kotlin.time.Duration

abstract class FuzzConfigDSL {

    private val builder by lazy { KFConfig.fromSystemProperties() }

    private inner class KFConfigDelegate<T : Any>(
        propertySelector: KFConfigBuilder.KFConfigImpl.() -> KProperty<T>
    ) {
        private val kfProp = builder.getPropertyDelegate(propertySelector)

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
            kfProp.getValue(thisRef, property)

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            builder.editFallback { kfProp.setValue(thisRef, property, value) }
        }
    }

    // global
    var workDir: Path by KFConfigDelegate { global::workDir }

    // target
    var maxTime: Duration by KFConfigDelegate { target::maxTime }

    // engine

//    var engine: EngineConfigDSL

    // jacoco

    fun JazzerEngine(jazzerConfig: JazzerConfigDSL.() -> Unit): EngineConfigDSL {
        val jazzerConfigDsl = object : JazzerConfigDSL {
            override var enableLogging = JazzerConfig.Defaults.ENABLE_LOGGING
            override var libFuzzerRssLimit = JazzerConfig.Defaults.LIB_FUZZER_RSS_LIMIT
        }
        return jazzerConfigDsl.apply(jazzerConfig)
    }

    fun build(): KFConfig = builder.build()
}

interface GlobalConfigDSL {
    var workDir: Path
    var reproducerPath: Path
    var logLevel: Level
    var applyHooks: Boolean
}

interface TargetConfigDSL {
    var keepGoing: Long
    var instrument: List<String>
    var customHookExcludes: List<String>
    var dumpCoverage: Boolean
}

interface EngineConfigDSL

interface JazzerConfigDSL : EngineConfigDSL {
    var libFuzzerRssLimit: Int
    var enableLogging: Boolean
}



