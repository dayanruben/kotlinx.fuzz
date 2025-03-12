package kotlinx.fuzz.reproduction

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.squareup.kotlinpoet.*
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

class ListAnyInlineReproducerWriter(
    private val template: ReproducerTestTemplate,
    private val instance: Any,
    private val method: Method,
    private val files: List<Path>,
) : CrashReproducerWriter(template, method) {
    private val relevantFunction = findRelevantFunction()
        ?: throw RuntimeException("Couldn't find file with method: ${method.name} in ${files.joinToString(", ") { it.absolutePathString() }}")

    private fun getAllChildren(ktElement: PsiElement): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        ktElement.children.forEach {
            result.add(it)
            result.addAll(getAllChildren(it))
        }
        return result
    }

    private fun findRelevantFunction(): KtNamedFunction? = files.filter { it.extension == "kt" }
        .map { file ->
            val project = KotlinCoreEnvironment.createForProduction(
                Disposer.newDisposable("Disposable for dummy project"),
                CompilerConfiguration(),
                EnvironmentConfigFiles.JVM_CONFIG_FILES,
            ).project
            val ktFile = PsiFileFactoryImpl(project).createFileFromText(
                file.name,
                KotlinLanguage.INSTANCE,
                file.readText(),
            ) as KtFile
            return@map getAllChildren(ktFile).filterIsInstance<KtNamedFunction>()
                .find { it.fqName?.asString() == "${method.declaringClass.name}.${method.name}" }
        }
        .filterNotNull()
        .getOrNull(0)

    @OptIn(ExperimentalStdlibApi::class)
    override fun writeToFile(input: ByteArray, reproducerFile: Path) {
        val parameterName = relevantFunction.valueParameters[0].name!!
        val body = relevantFunction.bodyExpression!!.text.trimIndent()
            .drop(1)
            .dropLast(1)
            .split("\n")
            .filter { it.isNotBlank() }
        val commonBlankPrefixLength = body.minOf { line -> line.takeWhile { it.isWhitespace() }.length }

        val hash = MessageDigest.getInstance("SHA-1").digest(input).toHexString()
        val code = buildCodeBlock {
            addStatement("${method.getInstanceString()}.`${method.name} reproducer $hash`()")
        }

        val extension = FunSpec.builder("`${method.name} reproducer $hash`")
            .receiver(method.declaringClass)
            .addModifiers(KModifier.PRIVATE)
            .addCode(
                buildCodeBlock {
                    addStatement(
                        "val $parameterName = ListReproducer(listOf<Any?>(${
                            registerOutputs(instance, method, input).joinToString(", ") { executionResult ->
                                executionResult.value?.let {
                                    if (executionResult.typeName.contains("Array")) {
                                        arrayToString(executionResult)
                                    } else {
                                        executionResult.value.toString()
                                    }
                                } ?: "null"
                            }
                        }))")
                    body.forEach { addStatement(it.drop(commonBlankPrefixLength)) }
                })
            .build()

        reproducerFile.writeText(
            template.buildReproducer(
                hash,
                code,
                additionalClasses = listOf(buildListReproducerObject()),
                additionalFunctions = listOf(extension)
            ),
        )
    }
}
