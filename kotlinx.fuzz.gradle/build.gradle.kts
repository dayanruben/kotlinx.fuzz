plugins {
    id("kotlinx.fuzz.src-module")
    `kotlin-dsl`
    alias(libs.plugins.gradle.publish)
}

dependencies {
    implementation(project(":kotlinx.fuzz.engine"))
    implementation(project(":kotlinx.fuzz.api"))

    gradleApi()
    implementation(kotlin("reflect"))
    implementation(libs.junit.platform.engine)

    testImplementation(libs.junit.platform.testkit)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(project(":kotlinx.fuzz.jazzer"))
}

gradlePlugin {
    website = "https://github.com/JetBrains-Research/kotlinx.fuzz"
    vcsUrl = "https://github.com/JetBrains-Research/kotlinx.fuzz.git"
    plugins {
        create("kotlinx.fuzz") {
            id = "kotlinx.fuzz"
            displayName = "kotlinx.fuzz Gradle plugin"
            description = "Gradle plugin for using kotlinx.fuzz"
            tags = listOf("testing", "fuzzing")
            implementationClass = "kotlinx.fuzz.gradle.KFuzzPlugin"
        }
    }
}

val logLevelProperty = "kotlinx.fuzz.logging.level"
val loggerImplementationProperty = "kotlinx.fuzz.logger.implementation"
val gradleLogger = "kotlinx.fuzz.gradle.GradleLogger"

// default property values
System.setProperty(loggerImplementationProperty, gradleLogger)

/**
 * Configures logging as in kotlinx.fuzz.gradle/src/main/kotlin/kotlinx/fuzz/gradle/KFuzzPlugin.kt
 * If changed, consider changing there as well
 */
tasks.test {
    val property = System.getProperty(logLevelProperty)
    systemProperties[logLevelProperty] = when {
        property?.uppercase() in LogLevel.values().map { it.name } -> property
        gradle.startParameter.logLevel == LogLevel.LIFECYCLE -> LogLevel.WARN.name
        else -> gradle.startParameter.logLevel.name
    }

    systemProperties[loggerImplementationProperty] = gradleLogger

    useJUnitPlatform {
        excludeEngines("kotlinx.fuzz")
    }
}
