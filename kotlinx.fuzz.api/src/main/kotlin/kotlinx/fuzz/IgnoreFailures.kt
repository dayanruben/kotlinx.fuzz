package kotlinx.fuzz

/**
 * Indicates that the annotated test is allowed to fail, but the failure will not be treated as an actual test failure.
 * Instead, it will be reported as passed
 */
@Target(AnnotationTarget.FUNCTION)
annotation class IgnoreFailures
