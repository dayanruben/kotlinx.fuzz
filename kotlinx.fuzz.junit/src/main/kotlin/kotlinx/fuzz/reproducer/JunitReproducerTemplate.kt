package kotlinx.fuzz.reproducer

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import java.lang.reflect.Method

class JunitReproducerTemplate(
    private val instance: Any,
    private val method: Method,
) : TestReproducerTemplate {
    override fun buildReproducer(
        identifier: String,
        testCode: CodeBlock,
        imports: List<String>,
        additionalCode: String,
    ): String {
        val fullClassName = instance::class.java.name
        val packageName = fullClassName.substringBeforeLast('.', missingDelimiterValue = "")

        val testFunction = FunSpec.Companion.builder("`${method.name} reproducer $identifier`")
            .addAnnotation(ClassName("org.junit.jupiter.api", "Test"))
            .returns(Unit::class)
            .addCode(testCode)
            .build()

        val objectSpec = TypeSpec.Companion.objectBuilder("Reproducer_$identifier")
            .addFunction(testFunction)
            .build()

        val fileSpec = FileSpec.Companion.builder(packageName, "")
            .addType(objectSpec)
        for (import in imports) {
            fileSpec.addImport(import.substringBeforeLast('.'), import.substringAfterLast('.'))
        }

        return "${fileSpec.build()}\n$additionalCode"
    }
}
