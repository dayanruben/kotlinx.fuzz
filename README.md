# kotlinx.fuzz

`kotlinx.fuzz` is a general purpose fuzzing library for Kotlin

## Usage

1. Add PLAN lab maven repository to your gradle config:

`build.gradle.kts`:
```kotlin
repositories {
    maven(url = "https://plan-maven.apal-research.com")
}
```
`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        maven(url = "https://plan-maven.apal-research.com")
    }
}
```


2. Add `kotlinx.fuzz` as a dependency:
```kotlin
dependencies {
    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer:0.1.0")
}
```

3. Apply `kotlinx.fuzz` plugin to your project:
```kotlin
plugins {
    id("kotlinx.fuzz.gradle") version "0.1.0"
}
```

4. Configure plugin:
```kotlin
fuzzConfig {
    instrument = listOf("org.example.**")
    maxSingleTargetFuzzTime = 10.seconds
}
```

5. Write your fuzz tests:
```kotlin
package org.example

import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer

object ExampleTest {
    @KFuzzTest
    fun foo(data: KFuzzer) {
        if (data.int() % 2 == 0) {
            if (data.int() % 3 == 2) {
                if (data.int() % 31 == 11) {
                    throw IllegalArgumentException()
                }
            }
        }
    }
}
```

6. Run fuzzer:
```bash
~/example » ./gradlew fuzz                                                                                                                                                  1 ↵

> Task fuzz

SampleTarget > public final void org.example.ExampleTest.foo(kotlinx.fuzz.KFuzzer) FAILED
    java.lang.IllegalArgumentException
        at org.example.ExampleTest.foo(ExampleTest.kt:12)
```

7. Check the fuzzing report in `build/fuzz`

You can see more examples of `kotlinz.fuzz` usage in [`kotlinx.fuzz.test`](kotlinx.fuzz.test)
