# `kotlinx.cli` bug report

Reproducers are found [here](src/test/kotlin/org/plan/research/reproduce).

## Unhandled `NoSuchElement- / ArrayIndexOutOfBoundsException`

When `--` is encountered in GNU mode, next entries are considered options. However, there is no check that there exist any.

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
