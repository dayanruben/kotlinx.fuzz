package kotlinx.fuzz.reproduction

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.squareup.kotlinpoet.CodeBlock
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*

class ListAnyInlineReproducerWriter(
    private val template: ReproducerTestTemplate,
    private val instance: Any,
    private val method: Method,
    private val files: List<Path>,
) : CrashReproducerWriter(template, method) {
    private val relevantFile = findRelevantFile()
        ?: throw RuntimeException("Couldn't find file with method: ${method.name} in ${files.joinToString(", ") { it.absolutePathString() }}")

    private fun getAllChildren(ktElement: PsiElement): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        ktElement.children.forEach {
            result.add(it)
            result.addAll(getAllChildren(it))
        }
        return result
    }

    private fun findRelevantFile(): Path? = files.filter { it.extension == "kt" }.find { file ->
        val project = KotlinCoreEnvironment.createForProduction(
            Disposer.newDisposable("Disposable for dummy project"),
            CompilerConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).project
        val ktFile = PsiFileFactoryImpl(project).createFileFromText(file.name, KotlinLanguage.INSTANCE, file.readText()) as KtFile
        val a = getAllChildren(ktFile).filterIsInstance<KtNamedFunction>().map { it.fqName?.asString() }
        return@find a.any { it == "${method.declaringClass.name}.${method.name}" }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun writeToFile(input: ByteArray, reproducerFile: Path) {
        reproducerFile.writeText(
            template.buildReproducer(
                MessageDigest.getInstance("SHA-1").digest(input).toHexString(),
                CodeBlock.builder().build(),
                additionalObjects = emptyList(),
            ),
        )
    }
}
