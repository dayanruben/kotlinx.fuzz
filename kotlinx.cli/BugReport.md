# `kotlinx.cli` bug report

Reproducers are found [here](src/test/kotlin/org/plan/research/reproduce).

## Unhandled `NoSuchElement- / ArrayIndexOutOfBoundsException`

When `--` is encountered in GNU mode, next entries are considered options. However, there is no check that there exist any.

Consider the following test. Many results can be considered correct:
* String "--" is interpreted as a delimiter between options and arguments, parsing is successful with no options and no arguments
* String "--" is interpreted as an argument and parsing is successful
* String "--" is interpreted as a start of the option and parser fails with a descriptive error because no options are registered

In practice though, it throws an `ArrayOutOfBoundsException`.

```kotlin
@Test
fun `gnu + '--'`() {
    val parser = ArgParser("").apply {
        argument(ArgType.String, fullName = "")
        prefixStyle = ArgParser.OptionPrefixStyle.GNU
    }

    val args = arrayOf("--")
    parser.parse(args)
}
```
