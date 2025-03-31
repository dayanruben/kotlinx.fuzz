package kotlinx.fuzz

import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FuzzingResult(val findingsNumber: Int, val time: Duration)

class FuzzingFoundErrorsException(message: String) : Throwable(message)

/**
 * Serializes [FuzzingResult] to the specified [path].
 *
 * @param result
 * @param path
 */
fun serializeFuzzingResult(result: FuzzingResult, path: Path) = path.writeText(Json.encodeToString(result))

/**
 * Reads a FuzzingResult from the specified [path].
 *
 * @param path
 */
fun deserializeFuzzingResult(path: Path): FuzzingResult = Json.decodeFromString(path.readText())
