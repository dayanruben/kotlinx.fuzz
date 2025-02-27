package kotlinx.fuzz.reproduction

import com.squareup.kotlinpoet.CodeBlock
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.writeText

class ByteArrayReproducerWriter(private val template: ReproducerTestTemplate, private val method: Method) : CrashReproducerWriter {
    @OptIn(ExperimentalStdlibApi::class)
    override fun writeToFile(input: ByteArray, reproducerFile: Path) {
        val instanceName = method.declaringClass.kotlin.objectInstance?.let {
            method.declaringClass.kotlin.simpleName
        } ?: "${method.declaringClass.kotlin.simpleName}::class.java.getDeclaredConstructor().newInstance()"

        val code = CodeBlock.builder()
            .addStatement(
                "$instanceName.`${method.name}`(KFuzzerImpl(byteArrayOf(${input.joinToString(", ")})))",
            )
            .build()

        reproducerFile.writeText(
            template.buildReproducer(
                MessageDigest.getInstance("SHA-1").digest(input).toHexString(),
                code,
                imports = listOf(KotlinpoetImport("kotlinx.fuzz", "KFuzzerImpl")),
            ),
        )
    }
}
