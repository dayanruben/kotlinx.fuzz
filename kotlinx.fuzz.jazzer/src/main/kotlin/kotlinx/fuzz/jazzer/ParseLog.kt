package kotlinx.fuzz.jazzer

import java.nio.file.Path
import kotlin.io.path.readLines
import kotlin.time.Duration

internal data class LibfuzzerLogEntryNoTimestamp(
    val execNr: Int,
    val cov: Int,
    val ft: Int,
    val crashes: Int,
) {
    fun addTimestamp(timeSeconds: Long): LibfuzzerLogEntry = LibfuzzerLogEntry(
        timeSeconds = timeSeconds,
        execNr = execNr,
        cov = cov,
        ft = ft,
        crashes = crashes,
    )
}

internal data class LibfuzzerLogEntry(
    val timeSeconds: Long,
    val execNr: Int,
    val cov: Int,
    val ft: Int,
    val crashes: Int,
)

internal fun jazzerLogToCsv(file: Path, duration: Duration): String {
    var crashes = 0

    val statsNoTimeStamps = file.readLines().mapNotNull { line ->
        val tokens = line.split("\\s+".toRegex())  // Split line into tokens
        when {
            tokens.size < 2 -> null

            tokens[0].startsWith("artifact_prefix=") -> {
                crashes++
                null
            }

            !tokens[0].startsWith("#") -> null
            @Suppress("MAGIC_NUMBER")
            tokens.size < 14 -> null

            tokens[1] !in setOf("NEW", "REDUCE", "pulse") -> null

            else ->
                @Suppress("MAGIC_NUMBER")
                LibfuzzerLogEntryNoTimestamp(
                    execNr = tokens[0].substring(1).toInt(),
                    cov = tokens[3].toInt(),
                    ft = tokens[5].toInt(),
                    crashes = crashes,
                )
        }
    }

    val maxExecNr = statsNoTimeStamps.maxOf { it.execNr }
    val stats = listOf(LibfuzzerLogEntry(0, 0, 0, 0, 0)) +
        statsNoTimeStamps.map { it.addTimestamp(it.execNr * duration.inWholeSeconds / maxExecNr) }

    @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")  // TODO: remove after tweaking diktat
    val header = "execNr,timeSeconds,cov,ft,crashes\n"
    val rows = stats.joinToString(separator = "\n") { entry ->
        with(entry) { "$execNr,$timeSeconds,$cov,$ft,$crashes" }
    }
    return header + rows
}
