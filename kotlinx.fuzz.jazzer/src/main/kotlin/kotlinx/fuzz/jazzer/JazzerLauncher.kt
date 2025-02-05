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
import java.nio.file.Files
import java.nio.file.Path
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
import kotlinx.fuzz.log.LoggerFacade
import kotlinx.fuzz.log.debug
import kotlinx.fuzz.log.error

object JazzerLauncher {
    private val log = LoggerFacade.getLogger<JazzerLauncher>()
    private val config = KFuzzConfig.fromSystemProperties()
    private val jazzerConfig = JazzerConfig.fromSystemProperties()

    init {
        val codeLocation = this::class.java.protectionDomain.codeSource.location
        val libsLocation = codeLocation.toURI()
            .toPath()
            .toFile()
            .parentFile
        System.load("$libsLocation/${System.mapLibraryName("casr_adapter")}")
    }

    private external fun parseAndClusterStackTraces(rawStacktraces: List<String>): List<Int>

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
        val oldStackTrace = stackTrace

        val newThrowable = (when {
            message == null && filteredCause == null -> this::class.java.getConstructor().newInstance()

            message == null && filteredCause != null -> this::class.java.getConstructor(Throwable::class.java)
                .newInstance(filteredCause)

            message != null && filteredCause == null -> this::class.java.getConstructor(String::class.java)
                .newInstance(message)

            else -> this::class.java.getConstructor(String::class.java, Throwable::class.java)
                .newInstance(message, filteredCause)
        }).apply {
            stackTrace = oldStackTrace.takeWhile {
                it.className != JazzerTarget::class.qualifiedName && it.methodName != JazzerTarget::fuzzTargetOne.name
            }.toTypedArray()
        }

        return newThrowable
    }

    fun runTarget(instance: Any, method: Method): Throwable? {
        val reproducerPath =
            Path(Opt.reproducerPath.get(), method.declaringClass.simpleName, method.name).absolute()
        if (!reproducerPath.exists()) {
            reproducerPath.createDirectories()
        }

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
            if (isTerminalFinding(bytes, finding, reproducerPath)) {
                atomicFinding.set(finding)
            }
        }

        JazzerTarget.reset(MethodHandles.lookup().unreflect(method), instance)
        FuzzTargetRunner.startLibFuzzer(libFuzzerArgs)

        return atomicFinding.get()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun isTerminalFinding(bytes: ByteArray, finding: Throwable, reproducerPath: Path): Boolean {
        val hash = MessageDigest.getInstance("SHA-1").digest(bytes).toHexString()
        val file = reproducerPath.absolute().resolve("stacktrace-$hash")
        if (!file.exists()) {
            file.createFile()
            file.writeText(finding.filter().stackTraceToString())
        }
        return clusterCrashes(reproducerPath) >= Opt.keepGoing.get()
    }

    fun initJazzer() {
        Log.fixOutErr(System.out, System.err)

        Opt.hooks.setIfDefault(config.hooks)
        Opt.instrumentationIncludes.setIfDefault(config.instrument)
        Opt.customHookIncludes.setIfDefault(config.instrument)
        Opt.customHookExcludes.setIfDefault(config.customHookExcludes)
        Opt.reproducerPath.setIfDefault(config.reproducersDir.absolutePathString())
        Opt.keepGoing.setIfDefault(config.keepGoing)

        AgentInstaller.install(Opt.hooks.get())

        FuzzTargetHolder.fuzzTarget = FuzzTargetHolder.FuzzTarget(
            JazzerTarget::fuzzTargetOne.javaMethod,
            LifecycleMethodsInvoker.noop(JazzerTarget),
        )
    }

    private fun convertToJavaStyleStackTrace(kotlinStackTrace: String): String {
        val lines = kotlinStackTrace.lines()
        if (lines.isEmpty()) {
            return kotlinStackTrace
        }

        val firstLine = lines.first()
        val updatedFirstLine = if (firstLine.startsWith("Exception in thread \"main\"")) {
            firstLine
        } else {
            "Exception in thread \"main\" $firstLine"
        }

        return listOf(updatedFirstLine).plus(lines.drop(1)).joinToString("\n")
    }

    private fun clusterCrashes(directoryPath: Path): Int {
        val stacktraceFiles = directoryPath.listDirectoryEntries("stacktrace-*")

        val rawStackTraces = mutableListOf<String>()

        stacktraceFiles.forEach { file ->
            val lines = convertToJavaStyleStackTrace(Files.readString(file))
            rawStackTraces.add(lines)
        }

        val clusters = parseAndClusterStackTraces(rawStackTraces)
        val mapping = mutableMapOf<Int, Path>()

        clusters.forEachIndexed { index, cluster ->
            val stacktraceSrc = stacktraceFiles[index]
            val isOld = mapping.containsKey(cluster)

            if (!mapping.containsKey(cluster)) {
                mapping[cluster] = directoryPath.resolve("cluster-${stacktraceSrc.readLines().first().trim()}")
            }

            val clusterDir = directoryPath.resolve(mapping[cluster]!!)
            if (!clusterDir.exists()) {
                clusterDir.createDirectory()
            }

            stacktraceSrc.copyTo(clusterDir.resolve(stacktraceSrc.fileName), overwrite = true)
            if (isOld) {
                stacktraceSrc.deleteExisting()
            }
        }

        return clusters.max()
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
