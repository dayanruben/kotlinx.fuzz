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

tasks.test {
    val property = System.getProperty("kotlinx.fuzz.logging.level")
    systemProperties["kotlinx.fuzz.logging.level"] = if (property in LogLevel.values().map { it.name }) {
        property
    } else {
        if (gradle.startParameter.logLevel == LogLevel.LIFECYCLE) {
            LogLevel.WARN
        } else {
            gradle.startParameter.logLevel
        }
    }

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}
