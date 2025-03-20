package kotlinx.fuzz.reproducer

import com.squareup.kotlinpoet.*
import java.lang.reflect.Method
import java.security.MessageDigest

class ListAnyCallReproducerGenerator(
    template: TestReproducerTemplate,
    instance: Any,
    method: Method,
) : CrashReproducerGenerator(template, instance, method) {

    @OptIn(ExperimentalStdlibApi::class)
    override fun generate(seed: ByteArray): String {
        val code = buildCodeBlock {
            addStatement(
                "val values = listOf<Any?>(" +
                        extractTestData(seed).joinToString(", ") { toCodeString(it) } +
                        ")",
            )
            addStatement("${method.instanceString}.`${method.name}`(ValueReproducer(values))")
        }

        return template.buildReproducer(
            MessageDigest.getInstance("SHA-1").digest(seed).toHexString(),
            code,
            imports = listOf(
                "kotlinx.fuzz.KFuzzer",
                "kotlinx.fuzz.reproducer.ValueReproducer",
            )
        )
    }
}
