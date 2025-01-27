package kotlinx.fuzz.jazzer

import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.KFuzzEngine
import java.lang.reflect.Method
import kotlin.io.path.ExperimentalPathApi

@Suppress("unused")
class JazzerEngine(private val config: KFuzzConfig) : KFuzzEngine {
    private val jazzerConfig = JazzerConfig.fromSystemProperties()

    override fun initialise(): Unit = Unit


    @OptIn(ExperimentalPathApi::class)
    override fun runTarget(instance: Any, method: Method): Throwable? {
        // spawn subprocess, redirect output to log and err files

        val classpath = System.getProperty("java.class.path")
        val mainClass = "kotlinx.fuzz.jazzer.Launcher"
        val javaCommand = System.getProperty("java.home") + "/bin/java"

        // to pass `config`
        val properties = config.toPropertiesMap().map { (key, value) -> "-D$key=$value"}.toTypedArray()

        val pb = ProcessBuilder(
            javaCommand,
            "-classpath", classpath,
            *properties,
            mainClass,
            method.declaringClass.name, method.name,
        )

        pb.inheritIO()
        val res = pb.start().waitFor()
        if (res == 0) {
            return null
        }
        // TODO: read real exception
        return Throwable("Jazzer subprocess returned with code $res")
    }
}
