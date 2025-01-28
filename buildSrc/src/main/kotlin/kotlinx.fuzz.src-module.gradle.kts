import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

group = GROUP_ID
version = VERSION

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.getByName<KotlinCompile>("compileKotlin") {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

val logLevelProperty = "kotlinx.fuzz.logging.level"

tasks.test {
    val property = System.getProperty(logLevelProperty)
    systemProperties[logLevelProperty] = when {
        property?.uppercase() in LogLevel.values().map { it.name } -> property
        gradle.startParameter.logLevel == LogLevel.LIFECYCLE -> LogLevel.WARN
        else -> gradle.startParameter.logLevel
    }

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
    }
}
