# Logging in `kotlinx.fuzz`

`kotlinx.fuzz` allows user to enable logs during the fuzzing campaign. It uses SLF4J API to implement logging and can use user-provided SLF4J implementation to output the logs. If there are no SLF4J implementations provided, `kotlinx.fuzz` uses its custom logging implementation to output all the debugging info to standard output.

By default `kotlinx.fuzz` outputs only WARN or more severe messages, however this behaviour can be configured through `logLevel` property in the `fuzzConfig` or through `"kotlinx.fuzz.log.level"` system property.

Additionally, `kotlinx.fuzz` saves all the output/logs of the fuzzing engine. They are saved in the `"$workDir/logs"` directory, individually for each fuzz test. Additionally, user can enable printing of the Jazzer logs to standard output through `enableLogging` configuration option or `"kotlinx.fuzz.jazzer.enableLogging"` system property.
