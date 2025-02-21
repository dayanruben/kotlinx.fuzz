package kotlinx.fuzz.crash_reproduction

import java.lang.reflect.Method
import com.squareup.kotlinpoet.*
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText

object CrashReproducer {
    fun writeReproducer(instance: Any, method: Method, input: ByteArray, reproducerFile: Path) {
        val testFunction = FunSpec.builder("test")
            .addModifiers(KModifier.PUBLIC)
            .returns(Unit::class)
            .addStatement("// TODO: Implement test logic here")
            .build()

        val testObject = TypeSpec.objectBuilder("Reproducer${reproducerFile.nameWithoutExtension.removePrefix("reproducer-")}")
            .addFunction(testFunction)
            .build()

        val fileSpec = FileSpec.builder("com.example.generated", "TestFile")
            .addType(testObject)
            .build()

        reproducerFile.writeText(fileSpec.toString())
    }
}
