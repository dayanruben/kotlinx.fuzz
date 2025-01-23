package kotlinx.fuzz.jazzer

import com.code_intelligence.jazzer.agent.AgentInstaller
import com.code_intelligence.jazzer.driver.FuzzTargetHolder
import com.code_intelligence.jazzer.driver.FuzzTargetRunner
import com.code_intelligence.jazzer.driver.LifecycleMethodsInvoker
import com.code_intelligence.jazzer.driver.Opt
import com.code_intelligence.jazzer.utils.Log
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.reflect.jvm.javaMethod
import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.KFuzzEngine
import java.io.File
import java.security.MessageDigest
import kotlin.io.path.Path

class JazzerEngine(private val config: KFuzzConfig) : KFuzzEngine {
    private val jazzerConfig = JazzerConfig.fromSystemProperties()

    init {
        System.load("${Path("").toAbsolutePath()}/${System.mapLibraryName("casr_adapter")}")
    }

    private external fun parseAndClusterStackTraces(rawStacktraces: List<String>): List<Int>

    override fun initialise() {
        Log.fixOutErr(System.out, System.err)

        Opt.hooks.setIfDefault(config.hooks)
        Opt.instrument.setIfDefault(config.instrument)
        Opt.customHookExcludes.setIfDefault(config.customHookExcludes)

        AgentInstaller.install(Opt.hooks.get())

        FuzzTargetHolder.fuzzTarget = FuzzTargetHolder.FuzzTarget(
            JazzerTarget::fuzzTargetOne.javaMethod,
            LifecycleMethodsInvoker.noop(JazzerTarget),
        )
    }

    @OptIn(ExperimentalPathApi::class, ExperimentalStdlibApi::class)
    override fun runTarget(instance: Any, method: Method): Throwable? {
        val libFuzzerArgs = mutableListOf("fake_argv0")
        val corpusDir = createTempDirectory("jazzer-corpus")

        libFuzzerArgs += corpusDir.toString()
        libFuzzerArgs += "-max_total_time=${config.maxSingleTargetFuzzTime.inWholeSeconds}"
        libFuzzerArgs += "-rss_limit_mb=${jazzerConfig.libFuzzerRssLimit}"

        val atomicFinding = AtomicReference<Throwable>()
        FuzzTargetRunner.registerFatalFindingHandlerForJUnit { bytes, finding ->
            atomicFinding.set(finding)
            val hash = MessageDigest.getInstance("SHA-1").digest(bytes).toHexString()
            val file = File(Path(Opt.reproducerPath.get()).toAbsolutePath().toString(), "stacktrace-$hash")
            file.createNewFile()
            file.writeText(
                finding.stackTraceToString().split("\n")
                    .takeWhile { it.trim() != "at kotlinx.fuzz.jazzer.JazzerTarget.fuzzTargetOne(JazzerTarget.kt:17)" }
                    .joinToString("\n")
            )
        }

        JazzerTarget.reset(MethodHandles.lookup().unreflect(method), instance)
        FuzzTargetRunner.startLibFuzzer(libFuzzerArgs)

        corpusDir.deleteRecursively()

        return atomicFinding.get()
    }

    private fun convertToJavaStyleStackTrace(kotlinStackTrace: String): String {
        val lines = kotlinStackTrace.lines()
        if (lines.isEmpty()) return kotlinStackTrace

        val firstLine = lines.first()
        val updatedFirstLine = if (firstLine.startsWith("Exception in thread \"main\"")) {
            firstLine
        } else {
            "Exception in thread \"main\" $firstLine"
        }

        return listOf(updatedFirstLine).plus(lines.drop(1)).joinToString("\n")
    }

    override fun finishExecution() {
        val directoryPath = Path(Opt.reproducerPath.get()).toAbsolutePath().toString()
        val dir = File(directoryPath)

        val stacktraceFiles = dir.listFiles { file -> file.name.startsWith("stacktrace-") }
            ?: throw IllegalStateException("Unable to list files in directory: $directoryPath")

        val rawStackTraces = mutableListOf<String>()
        val fileMapping = mutableListOf<Pair<String, String>>()

        stacktraceFiles.forEach { file ->
            val crashFile = File(dir, "crash-${file.name.removePrefix("stacktrace-")}")
            val lines = convertToJavaStyleStackTrace(file.readText())
            rawStackTraces.add(lines)
            fileMapping.add(file.name to crashFile.name)
        }

        val clusters = parseAndClusterStackTraces(rawStackTraces)
        val mapping = mutableMapOf<Int, String>()

        clusters.forEachIndexed { index, cluster ->
            val (stacktraceFile, crashFile) = fileMapping[index]
            val stacktraceSrc = File(dir, stacktraceFile)
            val crashSrc = File(dir, crashFile)
            val isOld = mapping.containsKey(cluster)

            if (!mapping.containsKey(cluster)) {
                mapping[cluster] = "cluster-${stacktraceSrc.readLines().first().trim()}"
            }

            val clusterDir = File(dir, mapping[cluster]!!)
            if (!clusterDir.exists()) clusterDir.mkdir()

            val stacktraceDest = File(clusterDir, stacktraceFile)
            val crashDest = File(clusterDir, crashFile)

            stacktraceSrc.copyTo(stacktraceDest, overwrite = true)
            if (isOld) {
                stacktraceSrc.delete()
            }

            crashSrc.copyTo(crashDest, overwrite = true)
            if (isOld) {
                crashSrc.delete()
            }
        }
    }
}
