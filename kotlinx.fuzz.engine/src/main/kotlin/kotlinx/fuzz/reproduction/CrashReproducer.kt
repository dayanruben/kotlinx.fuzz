package kotlinx.fuzz.reproduction

import java.nio.file.Path

interface CrashReproducer {
    fun writeToFile(input: ByteArray, reproducerFile: Path)
}
