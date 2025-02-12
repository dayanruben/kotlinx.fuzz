import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.fuzz.configurePublishing

plugins {
    id("kotlinx.fuzz.src-module")
}

val rustTargets = listOf(
    "x86_64-unknown-linux-gnu",
    "aarch64-unknown-linux-gnu",
    "x86_64-apple-darwin",
    "aarch64-apple-darwin",
    "x86_64-pc-windows-gnu",
    "aarch64-pc-windows-msvc"
)

tasks.register<Exec>("buildRustLib") {
    workingDir = file("$projectDir/CasrAdapter")
    commandLine = listOf("bash", "build.sh")
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