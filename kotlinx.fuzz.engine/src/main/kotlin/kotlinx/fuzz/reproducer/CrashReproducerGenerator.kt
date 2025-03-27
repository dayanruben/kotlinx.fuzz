package kotlinx.fuzz.reproducer

import java.lang.reflect.Method
import java.nio.file.Path
import kotlin.io.path.writeText

abstract class CrashReproducerGenerator(
    val template: TestReproducerTemplate,
    val instance: Any,
    val method: Method,
) {
    protected val Method.instanceString: String
        get() = if (isDeclaredInObject()) {
            method.declaringClass.kotlin.simpleName.orEmpty()
        } else {
            "${method.declaringClass.kotlin.simpleName}()"
        }

    protected fun extractTestData(seed: ByteArray): List<Any?> {
        val fuzzer = ValueRegisteringKFuzzer(seed)
        try {
            method.invoke(instance, fuzzer)
        } catch (_: Throwable) {
            // Nothing to do here
        }
        return fuzzer.values
    }

    private fun Method.isDeclaredInObject() = this.declaringClass.kotlin.objectInstance != null

    abstract fun generate(seed: ByteArray): String

    fun generateToPath(seed: ByteArray, path: Path) {
        val code = generate(seed)
        path.writeText(code)
    }
}
