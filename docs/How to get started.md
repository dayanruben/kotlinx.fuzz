# How to get started

Let’s see how to get started with **kotlinx.fuzz** from the Kotlin standard library and go through all the steps that are required to fuzz your code.

1. **Analyze code**. Before starting the fuzzer, you should first decide _what it is that you want to fuzz_. Fuzzing every single function in your program is not very efficient, because it will require a lot of time and effort when analysing the bugs. Therefore, it is beneficial to select several (1-10) target functions that will be the main entry-points for fuzzer. In our case, we want to fuzz `Duration` class from Kotlin standard library. From all of it's API methods, we are going to focus on `parseIsoStringOrNull` method. It that parses a time duration from a string in the ISO-8601 format. If the string represents a valid time &mdash; it returns a Duration instance, otherwise it returns null. Let’s write a fuzz test that checks its correctness!
2. **Design a fuzz test**. After you selected your target, your next step is to decide how are you going to fuzz it. You need to decide what scenarios you want to test and how to generate all necessary data using fuzzer. In case of our example, `parseIsoStringOrNull` just takes a string in an ISO format. For the simplest case, lest just write a fuzz test that generates a random string and passes to the target:
```kotlin
@FuzzTest
fun testDuration(f: Fuzzer) {
    val isoString = f.generateString()
    val duration = Duration.parseIsoStringOrNull(isoString)
    println("$isoString -> $duration")
}
```
3. **Come up with an oracle**. After creating this simple test, we can run it for some time and manually analyse the results. However, it still takes quite a lot of effort. To fully leverage the power of fuzzing, we need to come up with an **oracle**: a way to automatically check if the execution result is correct. Oracle can be as simple or as complicated as you want, however, quality of an oracle decides what types of bugs you will be able to find. The simplest oracle is just an exception &mdash; you can ensure, that your program does not throw any unexpected exceptions. In some cases you can perform more complicated checks. For example, if you are fuzzing a JSON parsing library, you can do an inverse check: `toJSON(fromJSON(string)) == string`.  Luckily, in our example we can use the Java standard library, which has a method with exactly the same functionality (at least, according to the documentation 🙂). So here is what our final fuzz test looks like.
```kotlin
@FuzzTest
fun testDuration(f: Fuzzer) {
    val isoString = f.generateString()
    val duration = Duration.parseIsoStringOrNull(isoString)

    val javaDuration = try {
        java.time.Duration.parse(isoString)
    } catch (_: Throwable) { null }

    assertEquals(javaDuration?.toKotlinDuration(), duration)
}
```

4. **[Configure](Configuration.md) and run the tests**. You need to decide how long you want to run fuzz tests, what parts of your project fuzzer should target and where it should store all the results. Here is an example of the simplest configuration:
```kotlin
fuzzConfig {
    instrument = listOf("org.example.**")
    maxFuzzTimePerTarget = 10.minutes
    coverage {
        reportTypes = setOf(CoverageReportType.HTML, CoverageReportType.CSV)
    }
}
```
Then, you just need to start the fuzz test:
```bash
~/example » ./gradlew fuzz
```

5. **Analyze the results**. After fuzzer finishes its execution, you can focus on analysing the results. There are two main results that we recommend you focus on:
    * **Coverage**. `build/fuzz/jacoco-report` will contain JaCoCo coverage reports in the configured formats. We recommend you to analyse the coverage to understand if the fuzzer was able to cover all the parts of your program that you wanted to test.
    * **Bugs**. `build/fuzz/reproducers` will contain all the crashes found by fuzzer. You can analyse them by running the fuzzer in the regression mode (`./gradlew regression`). Additionally, you can debug each crash by running your fuzz test in IDE. 

