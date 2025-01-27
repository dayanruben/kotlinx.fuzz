package kotlinx.fuzz.jazzer

import com.code_intelligence.jazzer.agent.AgentInstaller
import com.code_intelligence.jazzer.driver.FuzzTargetHolder
import com.code_intelligence.jazzer.driver.FuzzTargetRunner
import com.code_intelligence.jazzer.driver.LifecycleMethodsInvoker
import com.code_intelligence.jazzer.driver.Opt
import com.code_intelligence.jazzer.utils.Log
import kotlinx.fuzz.KFuzzConfig
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaMethod
import kotlin.system.exitProcess

object Launcher {
    private val config = KFuzzConfig.fromSystemProperties()
    private val jazzerConfig = JazzerConfig.fromSystemProperties()

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 2) {
            println("Usage: <full.class.Name> <methodName>")
            exitProcess(1)
        }
        val className = args[0]
        val methodName = args[1]
        println("Running $className::$methodName")

        val targetClass = Class.forName(className).kotlin
        val targetMethod = targetClass.memberFunctions.single { it.name == methodName }.javaMethod!!
        val instance = targetClass.objectInstance ?: targetClass.primaryConstructor!!.call()

        initJazzer()


        val error = runTarget(instance, targetMethod)
        if (error != null) {
            System.err.println(error)
            exitProcess(1)
        }
        exitProcess(0)
    }


    @OptIn(ExperimentalPathApi::class)
    fun runTarget(instance: Any, method: Method): Throwable? {
        val libFuzzerArgs = mutableListOf("fake_argv0")
        val currentCorpus = config.corpusDir.resolve(method.fullName)
        currentCorpus.createDirectories()

        if (config.dumpCoverage) {
            val coverageFile = config.workDir
                .resolve("coverage").createDirectories()
                .resolve("${method.fullName}.exec").absolute().toString()
            Opt.coverageDump.setIfDefault(coverageFile)
        }

        libFuzzerArgs += currentCorpus.toString()
        libFuzzerArgs += "-max_total_time=${config.maxSingleTargetFuzzTime.inWholeSeconds}"
        libFuzzerArgs += "-rss_limit_mb=${jazzerConfig.libFuzzerRssLimit}"

        val atomicFinding = AtomicReference<Throwable>()
        FuzzTargetRunner.registerFatalFindingHandlerForJUnit { finding ->
            atomicFinding.set(finding)
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