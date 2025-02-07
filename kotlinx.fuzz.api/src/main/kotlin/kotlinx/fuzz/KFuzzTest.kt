package kotlinx.fuzz

import kotlin.time.Duration
import org.junit.platform.commons.annotation.Testable

/**
 *  This annotation is used to mark targets for fuzzing.
 *  Global config parameters can be overloaded here. See [KFuzzConfig] for all parameters' description.
 *
 *  @param keepGoing how many bugs to discover before finishing fuzzing. Default: 1
 *  @param maxFuzzTime max time to fuzz this target, as a string that [kotlin.time.Duration.parse] can recognize.
 *  Default: "1m"
 *  @param instrument glob patterns matching names of classes that should be instrumented for fuzzing.
 *  Will be added to global config.
 *  @param customHookExcludes glob patterns matching names of classes that should NOT be instrumented with hooks.
 *  Will be added to global config.
 *  @param dumpCoverage whether fuzzer should generate jacoco `.exec` files
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Testable
annotation class KFuzzTest(
    val keepGoing: Long = KFuzzConfigImpl.Companion.Defaults.KEEP_GOING,
    val maxFuzzTime: String = KFuzzConfigImpl.Companion.Defaults.MAX_SINGLE_TARGET_FUZZ_TIME_STRING,
    val instrument: Array<String> = [],
    val customHookExcludes: Array<String> = [],
    val dumpCoverage: Boolean = KFuzzConfigImpl.Companion.Defaults.DUMP_COVERAGE,
)

fun KFuzzConfig.addAnnotationParams(annotation: KFuzzTest): KFuzzConfig = KFuzzConfigImpl.fromAnotherConfig(this) {
    keepGoing = newUnlessDefault(
        old = this@addAnnotationParams.keepGoing,
        new = annotation.keepGoing,
        default = KFuzzConfigImpl.Companion.Defaults.KEEP_GOING,
    )
    maxSingleTargetFuzzTime = newUnlessDefault(
        old = this@addAnnotationParams.maxSingleTargetFuzzTime,
        new = Duration.parse(annotation.maxFuzzTime),
        default = Duration.parse(KFuzzConfigImpl.Companion.Defaults.MAX_SINGLE_TARGET_FUZZ_TIME_STRING),
    )
    instrument = this@addAnnotationParams.instrument + annotation.instrument
    customHookExcludes = this@addAnnotationParams.customHookExcludes + annotation.customHookExcludes
    dumpCoverage = newUnlessDefault(
        old = this@addAnnotationParams.dumpCoverage,
        new = annotation.dumpCoverage,
        default = KFuzzConfigImpl.Companion.Defaults.DUMP_COVERAGE,
    )
}

private fun <T> newUnlessDefault(old: T, new: T, default: T): T = if (new == default) old else new
