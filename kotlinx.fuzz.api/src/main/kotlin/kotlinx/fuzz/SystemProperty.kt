package kotlinx.fuzz

enum class SystemProperty(name: String) {
    ENGINE("kotlinx.fuzz.engine"),
    HOOKS("kotlinx.fuzz.hooks"),
    KEEP_GOING("kotlinx.fuzz.keepGoing"),
    INSTRUMENT("kotlinx.fuzz.instrument"),
    CUSTOM_HOOK_EXCLUDES("kotlinx.fuzz.customHookExcludes"),
    MAX_SINGLE_TARGET_FUZZ_TIME("kotlinx.fuzz.maxSingleTargetFuzzTime"),
    WORK_DIR("kotlinx.fuzz.workDir"),
    DUMP_COVERAGE("kotlinx.fuzz.dumpCoverage"),
    REPRODUCER_PATH("kotlinx.fuzz.reproducerPath"),
    LOG_LEVEL("kotlinx.fuzz.log.level"),
    INTELLIJ_DEBUGGER_DISPATCH_PORT("idea.debugger.dispatch.port"),
    JAZZER_LIBFUZZERARGS_RSS_LIMIT_MB("kotlinx.fuzz.jazzer.libFuzzerArgs.rssLimitMb"),
    JAZZER_ENABLE_LOGGING("kotlinx.fuzz.jazzer.enableLogging"),
    REGRESSION("kotlinx.fuzz.regression");

    fun get(): String? {
        return System.getProperty(name)
    }

    fun get(default: String): String {
        return System.getProperty(name, default)
    }
}
