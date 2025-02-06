package kotlinx.fuzz.gradle

open class JacocoConfig(
    var html: Boolean = true,
    var xml: Boolean = false,
    var csv: Boolean = false,
    var includeDependencies: Set<String> = emptySet()
)