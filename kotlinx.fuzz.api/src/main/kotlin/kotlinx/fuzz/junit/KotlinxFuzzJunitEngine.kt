package kotlinx.fuzz.junit

import kotlinx.fuzz.KFuzzTest
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.ReflectionSupport
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.ClasspathRootSelector
import org.junit.platform.engine.discovery.MethodSelector
import org.junit.platform.engine.discovery.PackageSelector
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import java.net.URI


internal class KotlinxFuzzJunitEngine : TestEngine {
    val isKFuzzTestContainer: (Class<*>) -> Boolean = { klass ->
        AnnotationSupport.isAnnotated(klass, KFuzzTest::class.java)
    }

    override fun getId(): String = "kotlinx.fuzz"

    override fun discover(
        discoveryRequest: EngineDiscoveryRequest,
        uniqueId: UniqueId
    ): TestDescriptor? {
        println("discovering...")
        val engineDescriptor = EngineDescriptor(uniqueId, "kotlinx.fuzz")
        discoveryRequest.getSelectorsByType(ClasspathRootSelector::class.java).forEach { selector ->
            appendTestsInClasspathRoot(selector.classpathRoot, engineDescriptor)
        }

        discoveryRequest.getSelectorsByType(PackageSelector::class.java).forEach { selector ->
            appendTestsInPackage(selector!!.packageName, engineDescriptor)
        }

        discoveryRequest.getSelectorsByType(ClassSelector::class.java).forEach { selector ->
            appendTestsInClass(selector!!.getJavaClass(), engineDescriptor)
        }

        discoveryRequest.getSelectorsByType(MethodSelector::class.java).filter { methodSelector ->
            AnnotationSupport.isAnnotated(methodSelector.javaMethod, KFuzzTest::class.java)
        }
            .groupBy({ it.javaClass }) { it.javaMethod }
            .forEach { (javaClass, methods) ->
                val classDescriptor = ClassTestDescriptor(javaClass, engineDescriptor)
                engineDescriptor.addChild(classDescriptor)
                methods.forEach { method ->
                    classDescriptor.addChild(MethodTestDescriptor(method, classDescriptor))
                }
            }

        return engineDescriptor
    }

    private fun appendTestsInClasspathRoot(uri: URI, engineDescriptor: EngineDescriptor) {
        ReflectionSupport.findAllClassesInClasspathRoot(uri, isKFuzzTestContainer) { true }
            .map { klass -> ClassTestDescriptor(klass, engineDescriptor) }
            .forEach { testDescriptor -> engineDescriptor.addChild(testDescriptor) }
    }

    private fun appendTestsInPackage(packageName: String?, engineDescriptor: TestDescriptor) {
        ReflectionSupport.findAllClassesInPackage(
            packageName, isKFuzzTestContainer
        ) { true }
            .map { aClass -> ClassTestDescriptor(aClass!!, engineDescriptor) }
            .forEach { descriptor -> engineDescriptor.addChild(descriptor) }
    }

    private fun appendTestsInClass(javaClass: Class<*>, engineDescriptor: TestDescriptor) {
        engineDescriptor.addChild(ClassTestDescriptor(javaClass, engineDescriptor))
    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        configureJazzer()
        root.children.forEach { child -> executeImpl(request, child) }
    }

    private fun executeImpl(request: ExecutionRequest, descriptor: TestDescriptor) {
        when (descriptor) {
            is ClassTestDescriptor -> {
                request.engineExecutionListener.executionStarted(descriptor)
                descriptor.children.forEach { child -> executeImpl(request, child) }
                request.engineExecutionListener.executionFinished(
                    descriptor,
                    TestExecutionResult.successful()
                )
            }

            is MethodTestDescriptor -> {
                request.engineExecutionListener.executionStarted(descriptor)
                val method = descriptor.testMethod
                val instance = method.declaringClass.kotlin.objectInstance!!

                val finding = jazzerDoFuzzing(instance, method)
                val result = if (finding == null) {
                    TestExecutionResult.successful()
                } else {
                    TestExecutionResult.failed(finding)
                }
                request.engineExecutionListener.executionFinished(descriptor, result)
            }
        }
    }
}

