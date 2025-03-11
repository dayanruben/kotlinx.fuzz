package kotlinx.fuzz.reproduction

import com.squareup.kotlinpoet.CodeBlock
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.writeText

class ListAnyInlineReproducerWriter(
    private val template: ReproducerTestTemplate,
    private val instance: Any,
    private val method: Method,
    private val files: List<Path>,
) : CrashReproducerWriter(template, method) {
    private val relevantFile = findRelevantFile() ?: throw RuntimeException("Couldn't find file with method: ${method.name} in ${files.joinToString(", ") { it.absolutePathString() }}")

    private fun findRelevantFile(): Path? = files.filter { it.extension == "kt" }.find {
        it.exists()
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun writeToFile(input: ByteArray, reproducerFile: Path) {
        println(relevantFile.absolutePathString())
        reproducerFile.writeText(
            template.buildReproducer(
                MessageDigest.getInstance("SHA-1").digest(input).toHexString(),
                CodeBlock.builder().build(),
                additionalObjects = emptyList(),
            ),
        )
    }
}
