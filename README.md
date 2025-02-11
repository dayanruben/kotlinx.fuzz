# kotlinx.fuzz

`kotlinx.fuzz` is a general purpose fuzzing library for Kotlin. The library provides basic functionality:

* Simple API for writing fuzz tests
* Gradle plugin that provides an easy way of configuring the fuzzer, running it, and generating reports
* Custom JUnit engine that handles interactions with the fuzzing engine and allows for easy integration with IDE
* Integration with Jazzer as the main fuzzing engine for now

## Requirements

Currently, `kotlinx.fuzz` works only for JVM and requires JDK 17+ (will be updated to JDK 8+ in the future releases).

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

## Differences from Jazzer

`kotlinx.fuzz` uses Jazzer as the main fuzzing engine, but also introduces several new key features:

* Improved and simplified API
* Gradle plugin that integrates all the fuzzing-related tasks into your build system
* Improved crash deduplication algorithm
* Improved regression mode

## Trophy list

Trophy list can be found [here](docs/Trophy%20list.md)