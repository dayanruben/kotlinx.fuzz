package kotlinx.fuzz.jazzer

import com.code_intelligence.jazzer.agent.AgentInstaller
import com.code_intelligence.jazzer.driver.FuzzTargetHolder
import com.code_intelligence.jazzer.driver.FuzzTargetRunner
import com.code_intelligence.jazzer.driver.LifecycleMethodsInvoker
import com.code_intelligence.jazzer.driver.Opt
import com.code_intelligence.jazzer.utils.Log
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.*
import kotlin.reflect.jvm.javaMethod
import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.KFuzzEngine
import kotlinx.fuzz.RunMode

@Suppress("unused")
class JazzerEngine(private val config: KFuzzConfig) : KFuzzEngine {
    private val jazzerConfig = JazzerConfig.fromSystemProperties()

    override fun initialise() {
        Log.fixOutErr(System.out, System.err)

        Opt.hooks.setIfDefault(config.hooks)
        Opt.instrumentationIncludes.setIfDefault(config.instrument)
        Opt.customHookIncludes.setIfDefault(config.instrument)
        Opt.customHookExcludes.setIfDefault(config.customHookExcludes)
        Opt.reproducerPath.setIfDefault(config.reproducerPath.absolutePathString())

        AgentInstaller.install(Opt.hooks.get())

        FuzzTargetHolder.fuzzTarget = FuzzTargetHolder.FuzzTarget(
            JazzerTarget::fuzzTargetOne.javaMethod,
            LifecycleMethodsInvoker.noop(JazzerTarget),
        )
    }

    private fun configure(reproducerPath: Path): Pair<List<String>, Path> {
        val libFuzzerArgs = mutableListOf("fake_argv0")
        val corpusDir = createTempDirectory("jazzer-corpus")

        libFuzzerArgs += corpusDir.toString()
        libFuzzerArgs += "-rss_limit_mb=${jazzerConfig.libFuzzerRssLimit}"
        libFuzzerArgs += "-artifact_prefix=${reproducerPath.toAbsolutePath()}/"

        Opt.keepGoing.setIfDefault(
            when (config.runMode) {
                RunMode.REGRESSION -> {
                    libFuzzerArgs += "${reproducerPath.toAbsolutePath()}"
                    libFuzzerArgs += "-runs=${reproducerPath.listDirectoryEntries("crash-*").size}"

                    reproducerPath.listDirectoryEntries("crash-*").size.toLong()
                }

                RunMode.REGRESSION_FUZZING -> {
                    libFuzzerArgs += "${reproducerPath.toAbsolutePath()}"
                    libFuzzerArgs += "-max_total_time=${config.maxSingleTargetFuzzTime.inWholeSeconds}"

                    reproducerPath.listDirectoryEntries("crash-*").size + config.keepGoing.toLong()
                }

                RunMode.FUZZING -> {
                    libFuzzerArgs += "-max_total_time=${config.maxSingleTargetFuzzTime.inWholeSeconds}"

                    config.keepGoing.toLong()
                }
            },
        )

        return libFuzzerArgs to corpusDir
    }

    @OptIn(ExperimentalPathApi::class)
    override fun runTarget(instance: Any, method: Method): Throwable? {
        val reproducerPath =
            Paths.get(Opt.reproducerPath.get(), method.declaringClass.simpleName, method.name).absolute()
        if (!reproducerPath.exists()) {
            reproducerPath.createDirectories()
        }

        val (libFuzzerArgs, corpusDir) = configure(reproducerPath)

        val atomicFinding = AtomicReference<Throwable>()
        FuzzTargetRunner.registerFatalFindingHandlerForJUnit { finding ->
            atomicFinding.set(finding)
        }

        JazzerTarget.reset(MethodHandles.lookup().unreflect(method), instance)
        FuzzTargetRunner.startLibFuzzer(libFuzzerArgs)

        corpusDir.deleteRecursively()

        return atomicFinding.get()
    }
}
