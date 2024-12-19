package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.multiple
import kotlinx.cli.optional
import kotlinx.cli.required
import kotlinx.cli.vararg
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import kotlin.test.assertEquals

object CliComparisonTests {

    data class OptionsSetup(
        val kotlinx: ArgParser,
        val apache: Options
    )

    @FuzzTest(maxDuration = TEST_DURATION)
    fun comparisonTest(data: FuzzedDataProvider) {
        val programName = data.consumeAsciiString(5)
        val options = OptionsSetup(ArgParser(programName), Options())

        if (runCatching { options.setup(data) }.isFailure) return

        val args = List(data.consumeInt(0, 10)) { data.consumeAsciiString(10) }.toTypedArray()
        val apacheParser = DefaultParser.builder().build()
        val apacheResult = runCatching { apacheParser.parse(options.apache, args) }
        val kotlinxResult = runCatching { options.kotlinx.parse(args) }

        // huh? cannot make message when assert fails from a lambda? hmm
        val message = "Apache result: $apacheResult\nKotlinx result: $kotlinxResult"
        assertEquals(apacheResult.isFailure, kotlinxResult.isFailure, message)
        // TODO: compare parsed values? probably won't make a difference?
    }

    private fun OptionsSetup.setup(data: FuzzedDataProvider) {
        val optionsCount = data.consumeInt(1, 5)
        repeat(optionsCount) { addOption(data) }
        kotlinx.apply {
            disableExitProcess()
            argument(ArgType.String, "args").vararg().optional() // consume all unmatched arguments
        }
    }

    private fun OptionsSetup.addOption(data: FuzzedDataProvider) {
        val shortName = data.consumeAsciiString(2)
        val longName = data.consumeAsciiString(7)

        val hasValue = data.consumeBoolean()
        val hasMultipleValues = data.consumeBoolean()
        val isRequired = data.consumeBoolean()

        val apacheOpt = Option.builder(shortName)
            .longOpt(longName)
            .required(isRequired)
            .hasArg(hasValue)
            .runIf(hasMultipleValues) { hasArgs() }
            .build()
        apache.addOption(apacheOpt)

        with(kotlinx) {
            var opt = option(
                type = if (hasValue) ArgType.String else ArgType.Boolean,
                fullName = longName,
                shortName = shortName,
            )
            when {
                isRequired && hasMultipleValues -> opt.required().multiple()
                isRequired -> opt.required()
                hasMultipleValues -> opt.multiple()
                else -> opt
            }
        }
    }
}

fun <T> T.runIf(condition: Boolean, block: T.() -> T): T = if (condition) block() else this
