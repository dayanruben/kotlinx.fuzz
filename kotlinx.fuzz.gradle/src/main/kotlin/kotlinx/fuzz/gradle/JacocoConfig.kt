package kotlinx.fuzz.gradle

import kotlinx.fuzz.JacocoReport

open class JacocoConfig(
    var html: Boolean = true,
    var xml: Boolean = false,
    var csv: Boolean = false,
    var includeDependencies: Set<String> = emptySet()
) {
    internal fun reportTypes(): Set<JacocoReport> = buildSet {
        if (html) add(JacocoReport.HTML)
        if (xml) add(JacocoReport.XML)
        if (csv) add(JacocoReport.CSV)
    }
}
