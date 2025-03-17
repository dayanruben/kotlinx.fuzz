package kotlinx.fuzz.reproduction

import com.squareup.kotlinpoet.CodeBlock

interface ReproducerTestTemplate {
    fun buildReproducer(
        identifier: String,
        testCode: CodeBlock,
        imports: List<String> = emptyList(),
        additionalCode: String = "",
    ): String
}
