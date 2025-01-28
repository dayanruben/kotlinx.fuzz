package kotlinx.fuzz.jazzer

import java.nio.file.Path
import kotlin.io.path.forEachLine
import kotlin.time.Duration

internal data class LibfuzzerLogEntryNoTimestamp(
    val execNr: Int,
    val cov: Int,
    val ft: Int,
    val crashes: Int,
) {
    fun withTimestamp(timeSeconds: Long): LibfuzzerLogEntry = LibfuzzerLogEntry(
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
    val lines =
        mutableListOf(LibfuzzerLogEntryNoTimestamp(execNr = 0, cov = 0, ft = 0, crashes = 0))
    val durationSeconds = duration.inWholeSeconds.toLong()
    var crashes = 0

    file.forEachLine { line ->
        val tokens = line.split("\\s+".toRegex())  // Split line into tokens
        if (tokens.size < 2) {
            return@forEachLine
        }

        if (tokens[0].startsWith("artifact_prefix=")) {
            crashes++
            return@forEachLine
        } else if (tokens[0].startsWith("#") && tokens.size >= 14 &&
            (tokens[1] == "NEW" || tokens[1] == "REDUCE" || tokens[1] == "pulse")
        ) {
            val execs = tokens[0].substring(1).toInt()

            @Suppress("MAGIC_NUMBER")
            val covBlks = tokens[3].toInt()

            @Suppress("MAGIC_NUMBER")
            val covFt = tokens[5].toInt()

            lines += LibfuzzerLogEntryNoTimestamp(
                execNr = execs,
                cov = covBlks,
                ft = covFt,
                crashes = crashes,
            )
        }
    }

    val maxExecNr = lines.maxOf { it.execNr }
    val stats = lines.map { it.withTimestamp(it.execNr * durationSeconds / maxExecNr) }

    val header = "execNr,timeSeconds,cov,ft,crashes\n"
    val rows = stats.joinToString(separator = "\n") { entry ->
        with(entry) {
            "$execNr,$timeSeconds,$cov,$ft,$crashes"
        }
    }
    return header + rows
}
