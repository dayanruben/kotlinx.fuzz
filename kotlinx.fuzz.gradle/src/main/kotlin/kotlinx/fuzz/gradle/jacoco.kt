package kotlinx.fuzz.gradle

import java.io.File
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.tools.ExecFileLoader
import org.jacoco.report.DirectorySourceFileLocator
import org.jacoco.report.FileMultiReportOutput
import org.jacoco.report.IReportVisitor
import org.jacoco.report.MultiReportVisitor
import org.jacoco.report.html.HTMLFormatter

/**
 * Merges all JaCoCo .exec files in [execDir] into a single .exec result file at [result].
 *
 * @param execDir
 * @param result
 */
fun jacocoMerge(execDir: Path, result: Path) {
    // Use a single loader for merging. Each subsequent load() merges additional .exec data.
    val mergedLoader = ExecFileLoader()
    execDir.listDirectoryEntries("*.exec").forEach { execFile ->
        execFile.inputStream().buffered().use { mergedLoader.load(it) }
    }

    // Save merged execution data to the result .exec file
    result.outputStream().buffered().use { mergedLoader.save(it) }
}

/**
 * Generates an HTML coverage report (and optional XML) for [execFile].
 *
 * @param execFile Path to the .exec file
 * @param classPath Directory containing compiled .class files
 * @param sourceDirectories Directory containing source files (for line coverage information)
 * @param reportDir Output directory where the coverage report will be generated
 */
fun jacocoReport(
    execFile: Path,
    classPath: Set<File>,
    sourceDirectories: Set<File>,
    reportDir: Path,
) {
    // 1) Load execution data
    val execLoader = ExecFileLoader()
    execFile.inputStream().buffered().use { execLoader.load(it) }

    // 2) Analyze class files
    val coverageBuilder = CoverageBuilder()
    val analyzer = Analyzer(execLoader.executionDataStore, coverageBuilder)
    classPath.filter { it.exists() }.forEach { classFile ->
        analyzer.analyzeAll(classFile)
    }

    // 3) Create coverage bundle
    val bundle = coverageBuilder.getBundle("Coverage Report")

    // 4) Prepare report visitors (HTML, XML if desired)
    val htmlFormatter = HTMLFormatter()
    val htmlVisitor = htmlFormatter.createVisitor(FileMultiReportOutput(reportDir.toFile()))

    // If you want XML output as well, uncomment below:
    // val xmlFormatter = XMLFormatter()
    // val xmlVisitor = xmlFormatter.createVisitor(File(reportDir.toFile(), "jacoco.xml"))

    // Combine multiple visitors if needed
    val visitors = mutableListOf<IReportVisitor>(htmlVisitor)
    // visitors.add(xmlVisitor) // if you enable XML
    val reportVisitor = MultiReportVisitor(visitors)

    // htmlVisitor.visitInfo()
    // 5) Build the report
    reportVisitor.visitInfo(
        execLoader.sessionInfoStore.infos,
        execLoader.executionDataStore.contents,
    )
    sourceDirectories.forEach { directory ->
        reportVisitor.visitBundle(
            bundle,
            DirectorySourceFileLocator(directory, "UTF-8", 4),
        )
    }
    reportVisitor.visitEnd()
}
