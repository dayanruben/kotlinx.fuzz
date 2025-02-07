package kotlinx.fuzz.gradle.junit

import java.lang.reflect.Method
import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.methodReproducerPath
import kotlinx.fuzz.regression.listCrashes
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.MethodSource

internal class MethodTestDescriptor(
    val testMethod: Method, parent: TestDescriptor, config: KFuzzConfig,
) : AbstractTestDescriptor(
    parent.uniqueId.append("method", testMethod.name),
    displayName(testMethod),
    MethodSource.from(testMethod),
) {
    init {
        setParent(parent)
        if (isRegression()) {
            config.methodReproducerPath(testMethod).listCrashes().forEach { crashFile ->
                addChild(CrashTestDescriptor(testMethod, crashFile, this))
            }
        }
    }

    override fun getType(): TestDescriptor.Type = if (isRegression()) TestDescriptor.Type.CONTAINER else TestDescriptor.Type.TEST

    companion object {
        private fun displayName(testField: Method): String = try {
            testField.toString()
        } catch (_: IllegalAccessException) {
            "no name"
        }
    }
}
