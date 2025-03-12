package kotlinx.fuzz.reproduction

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.jvmErasure
import kotlinx.fuzz.KFuzzer

class ListAnyCallReproducerWriter(
    private val template: ReproducerTestTemplate,
    private val instance: Any,
    private val method: Method,
) : CrashReproducerWriter(template, method) {
    @OptIn(ExperimentalStdlibApi::class)
    override fun writeToFile(input: ByteArray, reproducerFile: Path) {
        val instanceString = method.getInstanceString()
        val code = buildCodeBlock {
            addStatement(
                "val values = listOf<Any?>(" +
                    registerOutputs(instance, method, input).joinToString(", ") { executionResult ->
                        executionResult.value?.let {
                            if (executionResult.typeName.contains("Array")) {
                                arrayToString(executionResult)
                            } else {
                                executionResult.value.toString()
                            }
                        } ?: "null"
                    } +
                    ")",
            )
            addStatement("$instanceString.`${method.name}`(ListReproducer(values))")
        }

        reproducerFile.writeText(
            template.buildReproducer(
                MessageDigest.getInstance("SHA-1").digest(input).toHexString(),
                code,
                additionalClasses = listOf(buildListReproducerObject()),
            ),
        )
    }
}
