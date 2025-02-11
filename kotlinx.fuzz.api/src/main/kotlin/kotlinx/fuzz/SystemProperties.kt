package kotlinx.fuzz

object SystemProperties {
    const val ENGINE = "kotlinx.fuzz.engine"
    const val HOOKS = "kotlinx.fuzz.hooks"
    const val KEEP_GOING = "kotlinx.fuzz.keepGoing"
    const val INSTRUMENT = "kotlinx.fuzz.instrument"
    const val CUSTOM_HOOK_EXCLUDES = "kotlinx.fuzz.customHookExcludes"
    const val MAX_SINGLE_TARGET_FUZZ_TIME= "kotlinx.fuzz.maxSingleTargetFuzzTime"
    const val WORK_DIR = "kotlinx.fuzz.workDir"
    const val DUMP_COVERAGE = "kotlinx.fuzz.dumpCoverage"
    const val RUN_MODES = "kotlinx.fuzz.runModes"
    const val REPRODUCER_PATH = "kotlinx.fuzz.reproducerPath"
    const val LOG_LEVEL = "kotlinx.fuzz.log.level"
    const val INTELLIJ_DEBUGGER_DISPATCH_PORT = "idea.debugger.dispatch.port"
    const val JAZZER_LIBFUZZERARGS_RSS_LIMIT_MB = "kotlinx.fuzz.jazzer.libFuzzerArgs.rssLimitMb"
    const val JAZZER_ENABLE_LOGGING = "kotlinx.fuzz.jazzer.enableLogging"
}