package kotlinx.fuzz.gradle

import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.KFuzzConfig.Companion.toPropertiesMap

class KFuzzConfigBuilder private constructor() {
    var fuzzEngine: String = KFuzzConfig.Companion.FUZZ_ENGINE_DEFAULT
    var hooks: Boolean = KFuzzConfig.Companion.HOOKS_DEFAULT
    var keepGoing: Int = KFuzzConfig.Companion.KEEP_GOING_DEFAULT
    var customHookExcludes: List<String> = KFuzzConfig.Companion.CUSTOM_HOOK_EXCLUDES_DEFAULT
    var maxSingleTargetFuzzTime: Duration by Delegates.notNull<Duration>()
    lateinit var instrument: List<String>

    fun build(): KFuzzConfig = KFuzzConfig(
        fuzzEngine = fuzzEngine,
        hooks = hooks,
        keepGoing = keepGoing,
        instrument = instrument,
        customHookExcludes = customHookExcludes,
        maxSingleTargetFuzzTime = maxSingleTargetFuzzTime,
    )

    companion object {
        internal fun build(block: KFuzzConfigBuilder.() -> Unit): KFuzzConfig =
            KFuzzConfigBuilder().apply(block).build()

        internal fun writeToSystemProperties(block: KFuzzConfigBuilder.() -> Unit) {
            build(block).toPropertiesMap().forEach { (key, value) ->
                System.setProperty(key, value)
            }
        }
    }
}
