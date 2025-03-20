package kotlinx.fuzz.reproducer

import com.squareup.kotlinpoet.CodeBlock
import java.lang.reflect.Method
import java.security.MessageDigest

class ByteArrayReproducerGenerator(
    template: TestReproducerTemplate,
    instance: Any,
    method: Method,
) : CrashReproducerGenerator(template, instance, method) {

    @OptIn(ExperimentalStdlibApi::class)
    override fun generate(seed: ByteArray): String {
        val code = CodeBlock.builder()
            .addStatement(
                "${method.instanceString}.`${method.name}`(KFuzzerImpl(byteArrayOf(${seed.joinToString(", ")})))",
            )
            .build()

        return template.buildReproducer(
            MessageDigest.getInstance("SHA-1").digest(seed).toHexString(),
            code,
            imports = listOf("kotlinx.fuzz.KFuzzerImpl"),
        )
    }
}
