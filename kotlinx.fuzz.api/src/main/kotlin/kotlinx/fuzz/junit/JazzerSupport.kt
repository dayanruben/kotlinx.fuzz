package kotlinx.fuzz.junit

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
import kotlin.io.path.deleteRecursively
import kotlin.reflect.jvm.javaMethod

@OptIn(ExperimentalPathApi::class)
internal fun jazzerDoFuzzing(instance: Any, method: Method): Throwable? {

    val libFuzzerArgs = mutableListOf<String>("fake_argv0")
    val corpusDir = kotlin.io.path.createTempDirectory("jazzer-corpus")

    libFuzzerArgs += corpusDir.toString()
    libFuzzerArgs += "-max_total_time=10" // TODO: remove hardcoded args
    libFuzzerArgs += "-rss_limit_mb=0"

    val atomicFinding = AtomicReference<Throwable>()
    FuzzTargetRunner.registerFatalFindingHandlerForJUnit { finding ->
        println("fatal finding handler invoked")
        atomicFinding.set(finding)
    }

    JazzerTarget.reset(MethodHandles.lookup().unreflect(method), instance)
    val exitCode = FuzzTargetRunner.startLibFuzzer(libFuzzerArgs)

    corpusDir.deleteRecursively()

    return atomicFinding.get()
}


private var jazzerConfigured: Boolean = false
    set(value) {
        check(value != false)
        field = true
    }

internal fun configureJazzer() {
    if (jazzerConfigured) return
    jazzerConfigured = true

    Log.fixOutErr(System.out, System.err)

    Opt.hooks.setIfDefault(false)
    Opt.instrument.setIfDefault(listOf("kotlinx.fuzz.test.**"))
    Opt.customHookExcludes.setIfDefault(
        listOf(
            "com.google.testing.junit.**",
            "com.intellij.**",
            "org.jetbrains.**",
            "io.github.classgraph.**",
            "junit.framework.**",
            "net.bytebuddy.**",
            "org.apiguardian.**",
            "org.assertj.core.**",
            "org.hamcrest.**",
            "org.junit.**",
            "org.opentest4j.**",
            "org.mockito.**",
            "org.apache.maven.**",
            "org.gradle.**"
        )
    )

    AgentInstaller.install(Opt.hooks.get())

    FuzzTargetHolder.fuzzTarget = FuzzTargetHolder.FuzzTarget(
        JazzerTarget::fuzzTargetOne.javaMethod!!,
        LifecycleMethodsInvoker.noop(JazzerTarget)
    )
}
