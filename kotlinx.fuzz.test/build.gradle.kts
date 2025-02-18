import kotlinx.fuzz.gradle.fuzzConfig
import kotlin.time.Duration.Companion.seconds

plugins {
    kotlin("jvm") version "2.0.21"
    id("kotlinx.fuzz.gradle")
    kotlin("plugin.serialization") version "2.0.20"
}

repositories {
    mavenCentral()
    maven(url = "https://plan-maven.apal-research.com")
}

dependencies {
    testImplementation(kotlin("test")) // adds green arrow in IDEA (no idea why)
    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.7.3")
}

fuzzConfig {
    keepGoing = 10
    instrument = listOf(
        "kotlinx.fuzz.test.**",
        "kotlinx.collections.immutable.**",
        "kotlinx.serialization.**",
    )
    maxSingleTargetFuzzTime = 10.seconds
}

/*

 - DSL in plugin should be separated from data classes
 - is config a singleton initialized by plugin?
    - certainly not target config, but we can keep it like it is now
    - also in tests we define it manually via writeToSystemProperties()
    - so write everything into system props and then FuzzConfig.Engine.load()?
        - btw FuzzConfig.load() or FuzzConfig.Target.load() separately?
 - how exactly is it better (or even different) than current solution?
    - separated DSL -- ok

    - better data flow (arguably not important from outside)

plugin config:
fuzzing {
    // global params
    workDir = ...
    logLevel = ...

    // target params
    keepGoing = ...
    maxTime = ...

    engine = Jazzer {
        // jazzer params
        libFuzzerRssLimit = ...
    }

    coverageReport {
        // jacoco params (later kover?)
        html = true
        csv = false
    }
}

cli config:
./gradlew k.f.t:fuzz -Pkotlinx.fuzz.engine=jazzer -Pkotlinx.fuzz.jazzer.libFuzzerRssLimitMb=0

getting config instance:
KFuzzConfigBuilder.loadFromSystemProperties()
    .editOverride {
        // set values (via delegates)
        // override system props, needed in target-specific config
    }
    .editFallback {
        // set values (delegates will know not to override)
        // do not override system props, needed in plugin
    }
    .build() // delegates parse and validate -- the only source of ConfigurationException

 - create delegates via operator provideDelegate(), so we can register them without reflection across classes
 - this way we can keep separate config classes (like TargetConfig, JazzerConfig, etc.)
    - although we would prefer a single builder...

 - so we have
 interface KFuzzConfig {
    val workDir: Path
    val target: TargetConfig
 }
 - and then KFuzzConfigImpl : KFuzzConfig {
    override val workDir by newConfigProperty<Path>(
        // similar to current delegates
        fromString = ...
        toString = ...
    )
 - and then
 fun Builder.build(): KFuzzConfig {
    delegates.forEach { it.parse and validate() }
    return impl
 }


 */

jacocoReport {
    csv = true
    html = true
    xml = true
    includeDependencies = setOf(
        "org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm",
        "org.jetbrains.kotlinx:kotlinx-serialization-protobuf-jvm",
    )
}

kotlin {
    jvmToolchain(17)
}
