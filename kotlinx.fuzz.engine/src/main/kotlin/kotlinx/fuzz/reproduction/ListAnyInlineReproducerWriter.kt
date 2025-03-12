package kotlinx.fuzz.reproduction

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.PsiFileFactoryImpl
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

    private fun generateFunction(function: KFunction<*>): FunSpec {
        val returnType = function.returnType.jvmErasure.asTypeName()
        val isNullable = function.returnType.isMarkedNullable
        val castOperator = if (isNullable) "as?" else "as"

        val parameters = function.parameters.drop(1)

        val result = FunSpec.builder(function.name)
            .returns(returnType.copy(nullable = isNullable))
            .addModifiers(KModifier.OVERRIDE)

        parameters.forEach { param ->
            val paramType = param.type.jvmErasure.asTypeName()

            val resolvedParamType = if (param.type.arguments.isNotEmpty()) {
                val typeArguments = param.type.arguments
                val resolvedArguments = typeArguments.map {
                    it.type?.jvmErasure?.asTypeName() ?: TypeVariableName("T")
                }
                paramType.parameterizedBy(*resolvedArguments.toTypedArray())
            } else {
                paramType
            }

            val finalParamType = if (param.type.isMarkedNullable) {
                resolvedParamType.copy(nullable = true)
            } else {
                resolvedParamType
            }

            result.addParameter(param.name!!, finalParamType)
        }
        result.addStatement("return iterator.next() $castOperator ${returnType.copy(nullable = isNullable)}")
        return result.build()
    }

    private fun buildListReproducerObject(): TypeSpec {
        val result = TypeSpec.classBuilder("ListReproducer")
            .addSuperinterface(KFuzzer::class)
            .addModifiers(KModifier.PRIVATE)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("values", List::class.asClassName().parameterizedBy(ANY.copy(nullable = true)))
                    .build(),
            )
            .addProperty(
                PropertySpec.builder(
                    "iterator",
                    Iterator::class.asClassName().parameterizedBy(ANY.copy(nullable = true))
                ).initializer("values.iterator()").addModifiers(
                    KModifier.PRIVATE,
                )
                    .build()
            )
        for (function in KFuzzer::class.declaredFunctions) {
            result.addFunction(generateFunction(function))
        }
        return result.build()
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
