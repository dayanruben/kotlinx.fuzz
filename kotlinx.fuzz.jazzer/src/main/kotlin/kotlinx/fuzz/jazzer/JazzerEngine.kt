package kotlinx.fuzz.jazzer

import com.code_intelligence.jazzer.agent.AgentInstaller
import com.code_intelligence.jazzer.driver.FuzzTargetHolder
import com.code_intelligence.jazzer.driver.FuzzTargetRunner
import com.code_intelligence.jazzer.driver.LifecycleMethodsInvoker
import com.code_intelligence.jazzer.driver.Opt
import com.code_intelligence.jazzer.utils.Log
import kotlinx.fuzz.KFuzzEngine
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.reflect.jvm.javaMethod


class JazzerEngine: KFuzzEngine {
    private var jazzerConfigured = false
        set(value) {
            check(value)
            field = true
        }

    override fun initialise() {
        if (jazzerConfigured) return
        jazzerConfigured = true

        Log.fixOutErr(System.out, System.err)

        Opt.hooks.setIfDefault(System.getProperty("jazzer.hooks").toBoolean())
        Opt.instrument.setIfDefault(System.getProperty("jazzer.instrument").split(',').map(String::trim).filter(String::isNotEmpty))
        Opt.customHookExcludes.setIfDefault(System.getProperty("jazzer.instrument").split(',').map(String::trim).filter(String::isNotEmpty)) // TODO: Other settings

        AgentInstaller.install(Opt.hooks.get())

        FuzzTargetHolder.fuzzTarget = FuzzTargetHolder.FuzzTarget(
            JazzerTarget::fuzzTargetOne.javaMethod!!,
            LifecycleMethodsInvoker.noop(JazzerTarget)
        )
    }

    @OptIn(ExperimentalPathApi::class)
    override fun runTarget(instance: Any, method: Method): Throwable? {
        val libFuzzerArgs = mutableListOf("fake_argv0")
        val corpusDir = createTempDirectory("jazzer-corpus")

        libFuzzerArgs += corpusDir.toString()
        libFuzzerArgs += "-max_total_time=${System.getProperty("jazzer.libFuzzerArgs.max_total_time")}"
        libFuzzerArgs += "-rss_limit_mb=${System.getProperty("jazzer.libFuzzerArgs.rss_limit_mb")}" // TODO: Other settings

        val atomicFinding = AtomicReference<Throwable>()
        FuzzTargetRunner.registerFatalFindingHandlerForJUnit { finding ->
            println("fatal finding handler invoked")
            atomicFinding.set(finding)
        }

        JazzerTarget.reset(MethodHandles.lookup().unreflect(method), instance)
        FuzzTargetRunner.startLibFuzzer(libFuzzerArgs)

        corpusDir.deleteRecursively()

        return atomicFinding.get()
    }
}