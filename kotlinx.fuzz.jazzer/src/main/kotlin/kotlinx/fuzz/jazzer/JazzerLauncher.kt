package kotlinx.fuzz.jazzer

import com.code_intelligence.jazzer.agent.AgentInstaller
import com.code_intelligence.jazzer.driver.FuzzTargetHolder
import com.code_intelligence.jazzer.driver.FuzzTargetRunner
import com.code_intelligence.jazzer.driver.LifecycleMethodsInvoker
import com.code_intelligence.jazzer.driver.Opt
import com.code_intelligence.jazzer.utils.Log
import java.io.ObjectOutputStream
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.*
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaMethod
import kotlin.system.exitProcess
import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.KLoggerFactory

object JazzerLauncher {
    private val log = KLoggerFactory.getLogger(JazzerLauncher::class.java)
    private val config = KFuzzConfig.fromSystemProperties()
    private val jazzerConfig = JazzerConfig.fromSystemProperties()

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 2) {
            log.error { "Usage: <full.class.Name> <methodName>" }
            exitProcess(1)
        }
        // arg[0] - fully qualified name of the class containing fuzz target
        // arg[1] - method name of the fuzz target
        val className = args[0]
        val methodName = args[1]
        log.debug { "Running $className::$methodName" }

        val targetClass = Class.forName(className).kotlin
        val targetMethod = targetClass.memberFunctions.single { it.name == methodName }.javaMethod!!
        val instance = targetClass.objectInstance ?: targetClass.primaryConstructor!!.call()

        initJazzer()

        val error = runTarget(instance, targetMethod)
        error?.let {
            serializeException(error, config.exceptionPath(targetMethod))
            exitProcess(1)
        }
        exitProcess(0)
    }

    private fun Throwable.filter(): Throwable {
        val filteredCause = cause?.filter()

        val newThrowable = (try {
            this::class.java.getConstructor(String::class.java, Throwable::class.java)
                .newInstance(message, filteredCause)
        } catch (e: Exception) {
            try {
                cause?.let {
                    this::class.java.getConstructor(Throwable::class.java).newInstance(filteredCause)
                } ?: this::class.java.getConstructor(String::class.java).newInstance(message)
            } catch (e: Exception) {
                Throwable(message, filteredCause)
            }
        }).apply {
            stackTrace = stackTrace.takeWhile {
                it.className != JazzerTarget::class.qualifiedName && it.methodName != JazzerTarget::fuzzTargetOne.name
            }.toTypedArray()
        }

        return newThrowable
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun runTarget(instance: Any, method: Method): Throwable? {
        val libFuzzerArgs = mutableListOf("fake_argv0")
        val currentCorpus = config.corpusDir.resolve(method.fullName)
        currentCorpus.createDirectories()

        if (config.dumpCoverage) {
            val coverageFile = config.workDir
                .resolve("coverage")
                .createDirectories()
                .resolve("${method.fullName}.exec")
                .absolute()
                .toString()
            Opt.coverageDump.setIfDefault(coverageFile)
        }

        libFuzzerArgs += currentCorpus.toString()
        libFuzzerArgs += "-max_total_time=${config.maxSingleTargetFuzzTime.inWholeSeconds}"
        libFuzzerArgs += "-rss_limit_mb=${jazzerConfig.libFuzzerRssLimit}"

        val atomicFinding = AtomicReference<Throwable>()
        FuzzTargetRunner.registerFatalFindingHandlerForJUnit { bytes, finding ->
            atomicFinding.set(finding)
            val hash = MessageDigest.getInstance("SHA-1").digest(bytes).toHexString()
            val file = Paths.get(Opt.reproducerPath.get(), "stacktrace-$hash")
            if (!file.exists()) {
                file.createFile()
                file.writeText(finding.filter().stackTraceToString())
            }
        }

        JazzerTarget.reset(MethodHandles.lookup().unreflect(method), instance)
        FuzzTargetRunner.startLibFuzzer(libFuzzerArgs)

        return atomicFinding.get()
    }

    fun initJazzer() {
        Log.fixOutErr(System.out, System.err)

        Opt.hooks.setIfDefault(config.hooks)
        Opt.instrumentationIncludes.setIfDefault(config.instrument)
        Opt.customHookIncludes.setIfDefault(config.instrument)
        Opt.customHookExcludes.setIfDefault(config.customHookExcludes)

        AgentInstaller.install(Opt.hooks.get())

        FuzzTargetHolder.fuzzTarget = FuzzTargetHolder.FuzzTarget(
            JazzerTarget::fuzzTargetOne.javaMethod,
            LifecycleMethodsInvoker.noop(JazzerTarget),
        )
    }
}

/**
 * Serializes [throwable] to the specified [path].
 */
private fun serializeException(throwable: Throwable, path: Path) {
    path.outputStream().buffered().use { outputStream ->
        ObjectOutputStream(outputStream).use { objectOutputStream ->
            objectOutputStream.writeObject(throwable)
        }
    }
}
