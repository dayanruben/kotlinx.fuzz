package kotlinx.fuzz.junit

import org.junit.platform.engine.TestSource
import java.lang.reflect.Method

internal data class MethodSource(private val className: String?, private val methodName: String?) :
    TestSource {
    companion object {
        private const val serialVersionUID = 1L

        fun from(testMethod: Method): MethodSource {
            return MethodSource(
                testMethod.declaringClass.toGenericString(),
                testMethod.name
            )
        }
    }
}
