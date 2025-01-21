package kotlinx.fuzz.task.target

import java.io.File
import org.gradle.api.DefaultTask
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder

abstract class TargetTask : DefaultTask() {
    fun findTargets(directory: File): Set<String> {
        val cb = ConfigurationBuilder().addUrls(directory.toURI().toURL())
            .setScanners(Scanners.MethodsAnnotated).forPackage("org.plan.research")
        val reflections = Reflections(cb)
        val annotation = "com.code_intelligence.jazzer.junit.FuzzTest"
        /* Can't use `getMethodsAnnotatedWith`
        because it founds nothing,
        probably because of loading classes from the outside of the project */
        return reflections.store["MethodsAnnotated"]!![annotation]!!.map {
            it.trim().replaceAfter("(", "").replace("(", "")
        }.toSet()
    }
}
