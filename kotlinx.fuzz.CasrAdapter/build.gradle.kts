import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.fuzz.configurePublishing
import org.gradle.internal.os.OperatingSystem

plugins {
    id("kotlinx.fuzz.src-module")
}

fun isArm() = System.getProperty("os.arch").contains("aarch") || System.getProperty("os.arch").contains("arm")
fun isX86() = System.getProperty("os.arch").contains("x86") || System.getProperty("os.arch").contains("amd64")

val rustTargets = when {
    gradle.startParameter.taskNames.any { it.contains("publish") } -> listOf(
        "x86_64-unknown-linux-gnu",
        "aarch64-unknown-linux-gnu",
        "x86_64-apple-darwin",
        "aarch64-apple-darwin",
        "x86_64-pc-windows-gnu",
        "aarch64-pc-windows-msvc"
    )

    OperatingSystem.current().isLinux && isX86() -> listOf("x86_64-unknown-linux-gnu")
    OperatingSystem.current().isLinux && isArm() -> listOf("aarch64-unknown-linux-gnu")
    OperatingSystem.current().isMacOsX && isX86() -> listOf("x86_64-apple-darwin")
    OperatingSystem.current().isMacOsX && isArm() -> listOf("aarch64-apple-darwin")
    OperatingSystem.current().isWindows && isX86() -> listOf("x86_64-pc-windows-gnu")
    OperatingSystem.current().isWindows && isArm() -> listOf("aarch64-pc-windows-msvc")
    else -> throw IllegalStateException(
        "Unsupported combination of os: ${OperatingSystem.current()} and architecture: ${System.getProperty("os.arch")}"
    )
}

tasks.register<Exec>("buildRustLib") {
    workingDir = file("$projectDir/CasrAdapter")
    commandLine = listOf("bash", "build.sh") + rustTargets
    outputs.upToDateWhen {
        rustTargets.all { file("$projectDir/CasrAdapter/target/$it/release").exists() }
    }
}

fun File.listSharedLibs(): Array<File>? = listFiles { file ->
    file.extension in setOf("dylib", "so", "dll")
}

tasks.register("linkRustLib") {
    dependsOn("buildRustLib")
    doLast {
        val sourceDirs = rustTargets.map { file("$projectDir/CasrAdapter/target/$it/release") }
        val targetDir = file(layout.buildDirectory.dir("libs"))

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        sourceDirs.forEach { sourceDir ->
            if (!sourceDir.exists()) {
                throw GradleException("Source directory $sourceDir does not exist")
            }

            sourceDir.listSharedLibs()?.forEach { file ->
                val targetLink = targetDir.resolve("${file.parentFile.parentFile.name}-${file.name}")
                if (targetLink.exists()) {
                    targetLink.delete()
                }

                try {
                    Files.createSymbolicLink(targetLink.toPath(), file.toPath())
                } catch (e: UnsupportedOperationException) {
                    println("Warning: Symbolic links are not supported or failed to create. Falling back to file copy.")
                    Files.copy(file.toPath(), targetLink.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } catch (e: Exception) {
                    throw GradleException("Failed to handle file ${file.name}", e)
                }
            }
        }
    }
}

tasks.register<Jar>("packageJar") {
    dependsOn("linkRustLib")
    from(layout.buildDirectory.dir("libs")) {
        into("native")
    }
}

tasks.named("compileKotlin") {
    dependsOn("linkRustLib")
}

tasks.register<Exec>("cleanCargo") {
    workingDir = file("$projectDir/CasrAdapter")
    commandLine = listOf("/usr/bin/env", "cargo", "clean")
}

tasks.named("clean") {
    dependsOn("cleanCargo")
    doLast {
        val targetDir = file(layout.buildDirectory.dir("libs"))
        targetDir.listSharedLibs()?.forEach { it.delete() }
    }
}

configurePublishing()
