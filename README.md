# kotlinx.fuzz
Fuzzer for Kotlin libraries


# Setup instructions

1. Create a new submodule for your library. Add it into [`settings.gradle.kts`](./settings.gradle.kts):
```kotlin
include("my-module")
```
2. Use following `build.gradle.kts` template:
```kotlin
plugins {
    id("org.plan.research.kotlinx-fuzz-submodule")
}

dependencies {
    implementation("your.target.library:here:1.0.0")
}
```

You don't need to change anything else.
Our `org.plan.research.kotlinx-fuzz-submodule` plugin already declares everything necessary:
* jazzer dependency (version `0.22.1`)
* jazzer environment variables (mainly `JAZZER_FUZZ`)
* JUnit options
* `copyDependencies` task: before executing tests, Gradle will copy all your dependencies into `my-module/build/dependencies` folder

When running the [`run-experiment`](./scripts/run-experiment) script you can provide jars from `my-module/build/dependencies` as `--classfile` options for JaCoCo coverage computation.