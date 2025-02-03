package kotlinx.fuzz.test

import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod
import org.junit.platform.testkit.engine.EngineTestKit
import java.nio.file.Files
import kotlin.io.path.pathString
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Disabled
object BadTarget {

    // should fail in current process
    @KFuzzTest(maxFuzzTime = "not even a duration")
    fun notDuration(data: KFuzzer) {
        data.boolean()
    }

    // should fail in new process
    // TODO: fails with wrong error! (NoSuchFile instead of validation exception)
    @KFuzzTest(maxFuzzTime = "-10s")
    fun negativeDuration(data: KFuzzer) {
        data.boolean()
    }

    fun makeFQN(methodSimpleName: String) = "kotlinx.fuzz.test.BadTarget#$methodSimpleName(kotlinx.fuzz.KFuzzer)"
}

class Tests {

    private fun runMethodTest(methodFQN: String): TestExecutionResult {
        return EngineTestKit
            .engine("kotlinx.fuzz") // if wrong, will fail to load engine
            .selectors(selectMethod(methodFQN))
            .execute()
            .allEvents()
            .executions()
            .list()
            .first()
            .terminationInfo
            .executionResult!!
    }

    @BeforeEach
    fun setupSystemProperties() {
        // Cannot access KFuzzConfig from this separate project, so need to set these variables manually :/
        val props = mapOf(
            "kotlinx.fuzz.engine" to "jazzer",
            "kotlinx.fuzz.hooks" to "true",
            "kotlinx.fuzz.keepGoing" to "1",
            "kotlinx.fuzz.instrument" to "kotlinx.fuzz.test.**",
            "kotlinx.fuzz.customHookExcludes" to "[]",
            "kotlinx.fuzz.maxSingleTargetFuzzTime" to "10",
            "kotlinx.fuzz.workDir" to Files.createTempDirectory("kotlinx.fuzz.test workdir").pathString,
            "kotlinx.fuzz.dumpCoverage" to "true",
        )
        props.forEach { (k, v) -> System.setProperty(k, v) }
    }

    @Test
    fun notDurationTest() {
        val result = runMethodTest(BadTarget.makeFQN("notDuration"))
        assertEquals(TestExecutionResult.Status.FAILED, result.status)

        val exception = result.throwable.get().cause // main exception is JUnitException
        // TODO: custom ValidationException? otherwise this is poorly maintainable
        assertIs<IllegalStateException>(exception)
        assertContains(exception.message!!, "duration")
    }

    @Test
    fun negativeDurationTest() {
        val result = runMethodTest(BadTarget.makeFQN("negativeDuration"))
        assertEquals(TestExecutionResult.Status.FAILED, result.status)

        val exception = result.throwable.get().cause
        assertIs<IllegalStateException>(exception)
        assertContains(exception.message!!, "duration")
    }
}
