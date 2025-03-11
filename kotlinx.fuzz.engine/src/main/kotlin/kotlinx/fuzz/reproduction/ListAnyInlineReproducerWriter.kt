package kotlinx.fuzz.reproduction

import java.lang.reflect.Method
import java.nio.file.Path

class ListAnyInlineReproducerWriter(
    private val template: ReproducerTestTemplate,
    private val instance: Any,
    private val method: Method,
    private val files: List<Path>,
) : CrashReproducerWriter(template, method) {
    override fun writeToFile(input: ByteArray, reproducerFile: Path) {

    }
}
