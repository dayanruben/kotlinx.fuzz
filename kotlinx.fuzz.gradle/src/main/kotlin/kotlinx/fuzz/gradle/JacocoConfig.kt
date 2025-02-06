package kotlinx.fuzz.gradle

/**
 * @param html - Enable HTML report (default: true)
 * @param xml - Enable XML report (default: false)
 * @param csv - Enable CSV report (default: false)
 * @param includeDependencies - Dependencies to include in the report (default: empty set)
 */
open class JacocoConfig(
    var html: Boolean = true,
    var xml: Boolean = false,
    var csv: Boolean = false,
    var includeDependencies: Set<String> = emptySet(),
) {
    internal fun reportTypes(): Set<JacocoReport> = buildSet {
        if (html) {
            add(JacocoReport.HTML)
        }
        if (xml) {
            add(JacocoReport.XML)
        }
        if (csv) {
            add(JacocoReport.CSV)
        }
    }
}
