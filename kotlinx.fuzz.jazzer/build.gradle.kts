import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.fuzz.configurePublishing

plugins {
    id("kotlinx.fuzz.src-module")
}

repositories {
    mavenLocal()
}

dependencies {
    implementation(project(":kotlinx.fuzz.engine"))
    implementation(project(":kotlinx.fuzz.api"))
    implementation(kotlin("reflect"))
    implementation(libs.jazzer)
}

tasks.register<Exec>("buildRustLib") {
    workingDir = file("$projectDir/CasrAdapter")
    commandLine = listOf("cargo", "build", "--release")
}

tasks.register("linkRustLib") {
    dependsOn("buildRustLib")
    doLast {
        val sourceDir = file("$projectDir/CasrAdapter/target/release")
        val targetDir = file(layout.buildDirectory.dir("libs"))

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        if (!sourceDir.exists()) {
            throw GradleException("Source directory $sourceDir does not exist")
        }

        sourceDir.listFiles { file ->
            file.name.endsWith(".dylib") || file.name.endsWith(".so") || file.name.endsWith(".dll")
        }?.forEach { file ->
            val targetLink = targetDir.resolve(file.name)
            if (targetLink.exists()) {
                targetLink.delete()
            }

            try {
                Files.createSymbolicLink(targetLink.toPath(), file.toPath())
            } catch (e: UnsupportedOperationException) {
                println("Warning: Symbolic links are not supported or failed to create. Falling back to file copy.")
                Files.copy(file.toPath(), targetLink.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (e: Exception) {
                throw GradleException("Failed to handle file ${file.name}: ${e.message}")
            }
        }
    }
}


tasks.named("compileKotlin") {
    dependsOn("linkRustLib")
}

configurePublishing()
