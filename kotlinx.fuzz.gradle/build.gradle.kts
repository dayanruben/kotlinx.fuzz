plugins {
    id("kotlinx.fuzz.src-module")
    `kotlin-dsl`
    `maven-publish`
//    alias(libs.plugins.gradle.publish)
}

dependencies {
    implementation(project(":kotlinx.fuzz.engine"))
    implementation(project(":kotlinx.fuzz.api"))

    implementation(kotlin("reflect"))
    implementation(libs.junit.platform.engine)
    implementation(libs.jacoco.core)
    implementation(libs.jacoco.report)


    testImplementation(libs.junit.platform.testkit)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(project(":kotlinx.fuzz.jazzer"))
}

gradlePlugin {
    website = "https://github.com/JetBrains-Research/kotlinx.fuzz"
    vcsUrl = "https://github.com/JetBrains-Research/kotlinx.fuzz.git"
    plugins {
        create("kotlinx.fuzz.gradle") {
            id = project.name
            displayName = "kotlinx.fuzz Gradle plugin"
            description = "Gradle plugin for using kotlinx.fuzz"
            tags = listOf("testing", "fuzzing")
            implementationClass = "kotlinx.fuzz.gradle.KFuzzPlugin"
        }
    }
}

//val logLevelProperty = "kotlinx.fuzz.logging.level"
//val loggerImplementationProperty = "kotlinx.fuzz.logger.implementation"
//val gradleLogger = "kotlinx.fuzz.gradle.GradleLogger"

/**
 * Configures logging as in kotlinx.fuzz.gradle/src/main/kotlin/kotlinx/fuzz/gradle/KFuzzPlugin.kt
 * If changed, consider changing there as well
 */
tasks.test {
//    val property = System.getProperty(logLevelProperty)
//    systemProperties[logLevelProperty] = when {
//        property?.uppercase() in LogLevel.values().map { it.name } -> property
//        gradle.startParameter.logLevel == LogLevel.LIFECYCLE -> LogLevel.WARN.name
//        else -> gradle.startParameter.logLevel.name
//    }
//
//    val hasLogging = try {
//        URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray()).loadClass(gradleLogger)
//        true
//    } catch (_: ClassNotFoundException) {
//        false
//    }
//    if (hasLogging) {
//        System.setProperty(loggerImplementationProperty, gradleLogger)
//        systemProperties[loggerImplementationProperty] = gradleLogger
//    }

    useJUnitPlatform {
        excludeEngines("kotlinx.fuzz")
    }
}

/**
 * We need custom publishing setup here, as `gradlePlugin` automatically configures maven publication to include
 * necessary components
 */
publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }
        repositories {
            maven {
                url = uri("https://maven.pkg.github.com/plan-research/kotlin-maven")
                credentials {
                    username = project.findProperty("gpr.user")?.toString()
                        ?: System.getenv("MAVEN_REPOSITORY_LOGIN")
                    password = project.findProperty("gpr.token")?.toString()
                        ?: System.getenv("MAVEN_REPOSITORY_PASSWORD")
                }
            }
        }
    }
}
