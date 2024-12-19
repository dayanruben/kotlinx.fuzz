package org.plan.research.reproduce

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlin.test.Test

object CliReproducerTests {

    @Test
    fun `gnu + '--'`() {
        val parser = ArgParser("").apply {
            argument(ArgType.String, fullName = "")
            prefixStyle = ArgParser.OptionPrefixStyle.GNU
        }

        val args = arrayOf("--")
        parser.parse(args)
    }

}
