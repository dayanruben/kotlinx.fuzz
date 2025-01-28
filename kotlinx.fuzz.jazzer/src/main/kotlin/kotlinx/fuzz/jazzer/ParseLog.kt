package kotlinx.fuzz.jazzer

import java.nio.file.Path
import kotlin.io.path.forEachLine
import kotlin.reflect.full.memberProperties
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

private inline fun <reified T : Any> List<T>.toCsv(): String {
    require(this.isNotEmpty())

    val properties = T::class.memberProperties
    val header = properties.joinToString(separator = ",") { it.name }
    val rows = this.map { item ->
        properties.joinToString(separator = ",") { prop ->
            prop.get(item)?.toString() ?: error("$item doesnt have property $prop")
        }
    }

    return (listOf(header) + rows).joinToString(separator = "\n")
}

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

    val statsCsv = stats.toCsv()
    return statsCsv
}
