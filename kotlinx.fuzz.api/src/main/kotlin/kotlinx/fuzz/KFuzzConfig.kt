package kotlinx.fuzz

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Class that stores generals fuzzing configuration
 *
 * @param fuzzEngine - name of engine to be used. Default: "jazzer"
 * @param hooks - apply fuzzing instrumentation
 * @param keepGoing - how many bugs to discover before finishing fuzzing. Default: 1
 * @param instrument - glob patterns matching names of classes that should be instrumented for fuzzing
 * @param customHookExcludes - Glob patterns matching names of classes that should not be instrumented with hooks
 * (custom and built-in).
 * Default: empty list
 * @param maxSingleTargetFuzzTime - max time to fuzz a single target in seconds
 */
data class KFuzzConfig(
    val fuzzEngine: String = FUZZ_ENGINE_DEFAULT,
    val hooks: Boolean = HOOKS_DEFAULT,
    val keepGoing: Int = KEEP_GOING_DEFAULT,
    val instrument: List<String>,
    val customHookExcludes: List<String> = CUSTOM_HOOK_EXCLUDES_DEFAULT,
    val maxSingleTargetFuzzTime: Duration,
) {
    init {
        require(keepGoing >= 0) { "'keepGoing' must be non-negative" }
        require(maxSingleTargetFuzzTime.inWholeSeconds > 0) { "'maxSingleTargetFuzzTime' must be at least 1 second" }
    }

    companion object {
        const val FUZZ_ENGINE_DEFAULT = "jazzer"
        const val HOOKS_DEFAULT = false
        const val KEEP_GOING_DEFAULT = 1
        val CUSTOM_HOOK_EXCLUDES_DEFAULT: List<String> = emptyList()

        fun fromSystemProperties(): KFuzzConfig {
            return KFuzzConfig(
                fuzzEngine = System.getProperty("kotlinx.fuzz.engine") ?: FUZZ_ENGINE_DEFAULT,
                hooks = System.getProperty("kotlinx.fuzz.hooks")?.toBoolean() ?: HOOKS_DEFAULT,
                instrument = System.getProperty("kotlinx.fuzz.instrument")
                    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)
                    ?: error("'kotlinx.fuzz.instrument' property is not set"),

                customHookExcludes = System.getProperty("kotlinx.fuzz.customHookExcludes")
                    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)
                    ?: CUSTOM_HOOK_EXCLUDES_DEFAULT,

                maxSingleTargetFuzzTime =
                    System.getProperty("kotlinx.fuzz.maxSingleTargetFuzzTime")!!.toInt().seconds
            )
        }

        fun KFuzzConfig.toPropertiesMap(): Map<String, String> = mapOf(
            "kotlinx.fuzz.engine" to fuzzEngine,
            "kotlinx.fuzz.hooks" to hooks.toString(),
            "kotlinx.fuzz.instrument" to instrument.joinToString<String>(","),
            "kotlinx.fuzz.customHookExcludes" to customHookExcludes.joinToString<String>(","),
            "kotlinx.fuzz.maxSingleTargetFuzzTime" to maxSingleTargetFuzzTime.inWholeSeconds.toString(),
        )
    }
}
