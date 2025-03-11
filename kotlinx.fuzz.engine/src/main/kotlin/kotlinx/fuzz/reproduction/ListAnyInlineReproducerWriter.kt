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

        parameters.forEach { param ->
            val paramType = param.type.jvmErasure.asTypeName()

            val resolvedParamType = if (param.type.arguments.isNotEmpty()) {
                val typeArguments = param.type.arguments
                val resolvedArguments = typeArguments.map {
                    it.type?.jvmErasure?.asTypeName() ?: TypeVariableName("T")
                }
                paramType.parameterizedBy(*resolvedArguments.toTypedArray())
            } else {
                if (paramType.simpleName == "CharacterSet" || paramType.simpleName == "RegexConfiguration") {
                    TypeVariableName(paramType.simpleName)
                } else {
                    paramType
                }
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
            .addModifiers(KModifier.PRIVATE)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("values", List::class.asClassName().parameterizedBy(ANY.copy(nullable = true)))
                    .build(),
            )
            .addProperty(
                PropertySpec.builder(
                    "iterator",
                    Iterator::class.asClassName().parameterizedBy(ANY.copy(nullable = true)),
                ).initializer("values.iterator()").addModifiers(
                    KModifier.PRIVATE,
                )
                    .build(),
            )
        for (function in KFuzzer::class.declaredFunctions) {
            result.addFunction(generateFunction(function))
        }
        return result.build()
    }

    private fun buildRegexConfigurationConstructor() = FunSpec.constructorBuilder()
        .addParameters(
            listOf(
                ParameterSpec.builder("maxInfinitePatternLength", INT)
                    .defaultValue("100")
                    .build(),
                ParameterSpec.builder("caseInsensitive", BOOLEAN)
                    .defaultValue("false")
                    .build(),
                ParameterSpec.builder(
                    "allowedCharacters",
                    TypeVariableName("CharacterSet").copy(nullable = true),
                )
                    .defaultValue("null")
                    .build(),
                ParameterSpec.builder("allowedWhitespaces", TypeVariableName("CharacterSet"))
                    .defaultValue("CharacterSet.WHITESPACES")
                    .build(),
            ),
        )
        .build()

    private fun buildRegexConfigurationClass() = TypeSpec.classBuilder("RegexConfiguration")
        .addModifiers(KModifier.PRIVATE, KModifier.DATA)
        .primaryConstructor(buildRegexConfigurationConstructor())
        .addProperties(
            listOf(
                PropertySpec.builder("maxInfinitePatternLength", INT)
                    .initializer("maxInfinitePatternLength")
                    .build(),
                PropertySpec.builder("caseInsensitive", BOOLEAN)
                    .initializer("caseInsensitive")
                    .build(),
                PropertySpec.builder("allowedCharacters", TypeVariableName("CharacterSet").copy(nullable = true))
                    .initializer("allowedCharacters")
                    .build(),
                PropertySpec.builder("allowedWhitespaces", TypeVariableName("CharacterSet"))
                    .initializer("allowedWhitespaces")
                    .build(),
            ),
        )
        .addType(
            TypeSpec.companionObjectBuilder()
                .addProperty(
                    PropertySpec.builder("DEFAULT", TypeVariableName("RegexConfiguration"))
                        .initializer("RegexConfiguration()")
                        .build(),
                )
                .build(),
        )
        .build()

    private fun buildCharacterSetConstructor() = FunSpec.constructorBuilder()
        .addParameter(
            ParameterSpec.builder("ranges", SET.parameterizedBy(CharRange::class.asTypeName()))
                .defaultValue("emptySet()")
                .build(),
        )
        .addParameter(
            ParameterSpec.builder("symbols", SET.parameterizedBy(CHAR))
                .defaultValue("emptySet()")
                .build(),
        )
        .build()

    private fun buildItForCharacterSetProperties() = listOf(
        PropertySpec.builder("reachedSymbols", BOOLEAN)
            .initializer("false")
            .mutable()
            .build(),
        PropertySpec.builder(
            "currentRange",
            Iterator::class.asClassName().parameterizedBy(CharRange::class.asClassName()),
        )
            .initializer("ranges.iterator()")
            .mutable()
            .build(),
        PropertySpec.builder("currentSymbol", Iterator::class.asClassName().parameterizedBy(CHAR))
            .mutable()
            .initializer(
                CodeBlock.of(
                    "when {\n" +
                        "    currentRange.hasNext() -> currentRange.next().iterator()\n" +
                        "    else -> {\n" +
                        "        reachedSymbols = true\n" +
                        "        symbols.iterator()\n" +
                        "    }\n" +
                        "}",
                ),
            )
            .build(),
    )

    private fun buildItForCharacterSetFunctions() = listOf(
        FunSpec.builder("step")
            .addModifiers(KModifier.PRIVATE)
            .addCode(
                "while (!currentSymbol.hasNext()) {\n" +
                    "    when {\n" +
                    "        reachedSymbols -> return\n" +
                    "        currentRange.hasNext() -> currentSymbol = currentRange.next().iterator()\n" +
                    "        else -> {\n" +
                    "            reachedSymbols = true\n" +
                    "            currentSymbol = symbols.iterator()\n" +
                    "        }\n" +
                    "    }\n" +
                    "}",
            ).build(),
        FunSpec.builder("hasNext")
            .addModifiers(KModifier.OVERRIDE)
            .returns(BOOLEAN)
            .addCode("return currentSymbol.hasNext()\n")
            .build(),
        FunSpec.builder("next")
            .addModifiers(KModifier.OVERRIDE)
            .returns(CHAR)
            .addCode("return currentSymbol.next().also { step() }\n")
            .build(),
    )

    private fun buildItForCharacterSet() = TypeSpec.classBuilder("It")
        .addModifiers(KModifier.PRIVATE, KModifier.INNER)
        .addSuperinterface(Iterator::class.asClassName().parameterizedBy(CHAR))
        .addProperties(buildItForCharacterSetProperties())
        .addInitializerBlock(CodeBlock.of("step()"))
        .addFunctions(buildItForCharacterSetFunctions())
        .build()

    private fun buildCharacterSetInitializerBlock() = buildCodeBlock {
        addStatement("require(size > 0) { %S }", "Can't create an empty character set")
        addStatement(
            "require(ranges.all { it.step == 1 && (it.start < it.endInclusive) }) { %S }",
            "All ranges must be ascending and with step 1",
        )
    }

    private fun buildCharacterSetCompanionObject() = TypeSpec.companionObjectBuilder()
        .addProperties(
            listOf(
                PropertySpec.builder("US_LETTERS", TypeVariableName("CharacterSet"))
                    .initializer("CharacterSet(setOf('a'..'z', 'A'..'Z'))")
                    .build(),
                PropertySpec.builder("WHITESPACES", TypeVariableName("CharacterSet"))
                    .initializer("CharacterSet(setOf(' ', '\\t', '\\r', '\\n', '\\u000B'))")
                    .build(),
            ),
        )
        .build()

    private fun buildCharacterSetClass() = TypeSpec.classBuilder("CharacterSet")
        .addModifiers(KModifier.PRIVATE, KModifier.DATA)
        .primaryConstructor(buildCharacterSetConstructor())
        .addSuperinterface(ITERABLE.parameterizedBy(CHAR))
        .addProperties(
            listOf(
                PropertySpec.builder("ranges", SET.parameterizedBy(CharRange::class.asTypeName()))
                    .initializer("ranges")
                    .build(),
                PropertySpec.builder("symbols", SET.parameterizedBy(CHAR))
                    .initializer("symbols")
                    .build(),
                PropertySpec.builder("size", INT)
                    .initializer("ranges.sumOf { it.last - it.first + 1 } + symbols.size")
                    .build(),
            ),
        )
        .addInitializerBlock(buildCharacterSetInitializerBlock())
        .addFunctions(
            listOf(
                FunSpec.builder("contains")
                    .addModifiers(KModifier.OPERATOR)
                    .addParameter("char", CHAR)
                    .returns(BOOLEAN)
                    .addCode("return char in symbols || ranges.any { char in it }\n")
                    .build(),
                FunSpec.builder("iterator")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(Iterator::class.asClassName().parameterizedBy(CHAR))
                    .addCode("return It()\n")
                    .build(),
            ),
        )
        .addTypes(listOf(buildItForCharacterSet(), buildCharacterSetCompanionObject()))
        .build()

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

    private fun buildLetterFunction() = FunSpec.builder("letter")
        .addModifiers(KModifier.PRIVATE)
        .receiver(TypeVariableName("ListReproducer"))
        .returns(CHAR)
        .addStatement("return char(CharacterSet.US_LETTERS)")
        .build()

    private fun buildLetterOrNullFunction() = FunSpec.builder("letterOrNull")
        .addModifiers(KModifier.PRIVATE)
        .receiver(TypeVariableName("ListReproducer"))
        .returns(CHAR.copy(nullable = true))
        .addStatement("return if (boolean()) letter() else null")
        .build()

    private fun buildLettersFunction() = FunSpec.builder("letters")
        .addModifiers(KModifier.PRIVATE)
        .receiver(TypeVariableName("ListReproducer"))
        .addParameter("maxLength", INT)
        .returns(CHAR_ARRAY)
        .addStatement("require(maxLength > 0) { \"maxLength must be greater than 0\" }")
        .addStatement("val list = mutableListOf<Char>()")
        .beginControlFlow("while (list.size < maxLength)")
        .addStatement("list.add(letter())")
        .endControlFlow()
        .addStatement("return list.toCharArray()")
        .build()

    private fun buildLettersOrNullFunction() = FunSpec.builder("lettersOrNull")
        .addModifiers(KModifier.PRIVATE)
        .receiver(TypeVariableName("ListReproducer"))
        .addParameter("maxLength", INT)
        .returns(CHAR_ARRAY.copy(nullable = true))
        .addStatement("return if (boolean()) letters(maxLength) else null")
        .build()

    private fun buildAsciiStringFunction() = FunSpec.builder("asciiString")
        .addModifiers(KModifier.PRIVATE)
        .receiver(TypeVariableName("ListReproducer"))
        .addParameter("maxLength", INT)
        .returns(STRING)
        .addStatement("return string(maxLength, charset = Charsets.US_ASCII)")
        .build()

    private fun buildAsciiStringOrNullFunction() = FunSpec.builder("asciiStringOrNull")
        .addModifiers(KModifier.PRIVATE)
        .receiver(TypeVariableName("ListReproducer"))
        .addParameter("maxLength", INT)
        .returns(STRING.copy(nullable = true))
        .addStatement("return if (boolean()) asciiString(maxLength) else null")
        .build()

    private fun buildRemainingAsAsciiStringFunction() = FunSpec.builder("remainingAsAsciiString")
        .addModifiers(KModifier.PRIVATE)
        .receiver(TypeVariableName("ListReproducer"))
        .returns(STRING)
        .addStatement("return remainingAsString(charset = Charsets.US_ASCII)")
        .build()

    private fun buildLetterStringFunction() = FunSpec.builder("letterString")
        .addModifiers(KModifier.PRIVATE)
        .receiver(TypeVariableName("ListReproducer"))
        .addParameter("maxLength", INT)
        .returns(STRING)
        .addStatement("return string(maxLength, charset = CharacterSet.US_LETTERS)")
        .build()

    private fun buildLetterStringOrNullFunction() = FunSpec.builder("letterStringOrNull")
        .addModifiers(KModifier.PRIVATE)
        .receiver(TypeVariableName("ListReproducer"))
        .addParameter("maxLength", INT)
        .returns(STRING.copy(nullable = true))
        .addStatement("return if (boolean()) letterString(maxLength) else null")
        .build()

    private fun buildRemainingAsLetterStringFunction() = FunSpec.builder("remainingAsLetterString")
        .addModifiers(KModifier.PRIVATE)
        .receiver(TypeVariableName("ListReproducer"))
        .returns(STRING)
        .addStatement("return remainingAsString(charset = CharacterSet.US_LETTERS)")
        .build()

    private fun buildPickFromCollectionFunction() = FunSpec.builder("pick")
        .addModifiers(KModifier.PRIVATE)
        .addTypeVariable(TypeVariableName("T"))
        .receiver(TypeVariableName("ListReproducer"))
        .addParameter("collection", COLLECTION.parameterizedBy(TypeVariableName("T")))
        .returns(TypeVariableName("T"))
        .addStatement("require(collection.isNotEmpty()) { \"collection is empty\" }")
        .addStatement("return collection.elementAt(int(collection.indices))")
        .build()

    private fun buildPickFromGenericArrayFunction() = FunSpec.builder("pick")
        .addModifiers(KModifier.PRIVATE)
        .addTypeVariable(TypeVariableName("T"))
        .receiver(TypeVariableName("ListReproducer"))
        .addParameter("array", ARRAY.parameterizedBy(TypeVariableName("T")))
        .returns(TypeVariableName("T"))
        .addStatement("require(array.isNotEmpty()) { \"array is empty\" }")
        .addStatement("return array[int(array.indices)]")
        .build()

    private fun buildPickFromArrayFunction(type: String) = FunSpec.builder("pick")
        .addModifiers(KModifier.PRIVATE)
        .receiver(TypeVariableName("ListReproducer"))
        .addParameter("array", TypeVariableName(type))
        .returns(TypeVariableName(type.removeSuffix("Array")))
        .addStatement("require(array.isNotEmpty()) { \"array is empty\" }")
        .addStatement("return array[int(array.indices)]")
        .build()

    private fun buildListReproducerExtensions() = listOf(
        buildLetterFunction(),
        buildLetterOrNullFunction(),
        buildLettersFunction(),
        buildLettersOrNullFunction(),
        buildAsciiStringFunction(),
        buildAsciiStringOrNullFunction(),
        buildRemainingAsAsciiStringFunction(),
        buildLetterStringFunction(),
        buildLetterStringOrNullFunction(),
        buildRemainingAsLetterStringFunction(),
        buildPickFromCollectionFunction(),
        buildPickFromGenericArrayFunction(),
        buildPickFromArrayFunction("BooleanArray"),
        buildPickFromArrayFunction("ByteArray"),
        buildPickFromArrayFunction("ShortArray"),
        buildPickFromArrayFunction("IntArray"),
        buildPickFromArrayFunction("LongArray"),
        buildPickFromArrayFunction("DoubleArray"),
        buildPickFromArrayFunction("FloatArray"),
        buildPickFromArrayFunction("CharArray"),
    )

    private fun buildCharacterSetExtensions() = listOf(
        FunSpec.builder("CharacterSet")
            .addModifiers(KModifier.PRIVATE)
            .addParameter(ParameterSpec("ranges", CharRange::class.asClassName(), KModifier.VARARG))
            .returns(TypeVariableName("CharacterSet"))
            .addStatement("return CharacterSet(ranges = ranges.toSet())")
            .build(),
        FunSpec.builder("CharacterSet")
            .addModifiers(KModifier.PRIVATE)
            .addParameter(ParameterSpec("symbols", SET.parameterizedBy(CHAR)))
            .returns(TypeVariableName("CharacterSet"))
            .addStatement("return CharacterSet(ranges = emptySet(), symbols = symbols)")
            .build(),
        FunSpec.builder("CharacterSet")
            .addModifiers(KModifier.PRIVATE)
            .addParameter(ParameterSpec("symbols", CHAR, KModifier.VARARG))
            .returns(TypeVariableName("CharacterSet"))
            .addStatement("return CharacterSet(ranges = emptySet(), symbols = symbols.toSet())")
            .build(),
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun writeToFile(input: ByteArray, reproducerFile: Path) {
        val parameterName = relevantFunction.valueParameters[0].name!!
        val body = relevantFunction.bodyExpression!!.text.trimIndent()
            .drop(1)
            .dropLast(1)
            .split("\n")
            .filter { it.isNotBlank() }
        val commonBlankPrefixLength = body.minOf { line -> line.takeWhile { it.isWhitespace() }.length }

        val code = buildCodeBlock {
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
        }

        reproducerFile.writeText(
            template.buildReproducer(
                MessageDigest.getInstance("SHA-1").digest(input).toHexString(),
                code,
                additionalClasses = listOf(
                    buildListReproducerObject(), buildCharacterSetClass(), buildRegexConfigurationClass(),
                ),
                additionalFunctions = buildListReproducerExtensions() + buildCharacterSetExtensions(),
            ),
        )
    }
}
