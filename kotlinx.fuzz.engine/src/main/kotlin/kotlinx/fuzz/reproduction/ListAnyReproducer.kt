package kotlinx.fuzz.reproduction

import com.squareup.kotlinpoet.CodeBlock
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*

class ListAnyReproducer(
    private val template: ReproducerTemplate,
    private val instance: Any,
    private val method: Method,
) : CrashReproducer {
    private fun arrayToString(executionResult: ExecutionResult): String =
        when {
            executionResult.typeName.startsWith("Boolean") ->
                "booleanArrayOf(${(executionResult.value as BooleanArray).joinToString(", ")})"

            executionResult.typeName.startsWith("Byte") ->
                "byteArrayOf(${(executionResult.value as ByteArray).joinToString(", ") { "$it as Byte" }})"

            executionResult.typeName.startsWith("Short") ->
                "shortArrayOf(${(executionResult.value as ShortArray).joinToString(", ") { "$it as Short" }})"

            executionResult.typeName.startsWith("Int") ->
                "intArrayOf(${(executionResult.value as IntArray).joinToString(", ")})"

            executionResult.typeName.startsWith("Long") ->
                "longArrayOf(${(executionResult.value as LongArray).joinToString(", ")})"

            executionResult.typeName.startsWith("Float") ->
                "floatArrayOf(${(executionResult.value as FloatArray).joinToString(", ") { "$it as Float" }})"

            executionResult.typeName.startsWith("Double") ->
                "doubleArrayOf(${(executionResult.value as DoubleArray).joinToString(", ")})"

            executionResult.typeName.startsWith("Char") ->
                "charArrayOf(${(executionResult.value as CharArray).joinToString(", ")})"

            else -> error("Unsupported execution result type: ${executionResult.typeName}")
        }

    @OptIn(ExperimentalStdlibApi::class)
    override fun writeToFile(input: ByteArray, reproducerFile: Path) {
        val initialCode = ""

        val code = CodeBlock.builder()
            .addStatement(
                "val values = mutableListOf<Any?>(${
                    registerOutputs(
                            instance,
                            method,
                            input,
                        ).joinToString(", ") { executionResult ->
                            executionResult.value?.let {
                                if (executionResult.typeName.contains("Array")) {
                                    arrayToString(executionResult)
                                } else {
                                    executionResult.value.toString()
                                }
                            } ?: "null"
                        }
                })",
            )
            .addStatement("var index = 0")
            .addStatement("val consumeNext = { values[index++] }")
            .addStatement(initialCode)
            .build()

        reproducerFile.writeText(
            template.buildReproducer(
                MessageDigest.getInstance("SHA-1").digest(input).toHexString(),
                code,
                listOf(KotlinpoetImport("kotlinx.fuzz", "KFuzzerImpl")),
            ),
        )
    }
}
