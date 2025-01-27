import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("kotlinx.fuzz.src-module")
}

repositories {
    mavenLocal()
}

dependencies {
    implementation(project(":kotlinx.fuzz.api"))
    implementation("com.code-intelligence:jazzer:0.0.0-dev")
    implementation(kotlin("reflect"))
}

tasks.register<Exec>("deployLocal") {
    workingDir = file("$rootDir/jazzer/deploy")
    commandLine = listOf("bash", "deploy_local.sh")
}

tasks.register<Exec>("buildRustLib") {
    workingDir = file("$rootDir/CasrAdapter")
    commandLine = listOf("cargo", "build", "--release")
}

tasks.register("linkRustLib") {
    dependsOn("buildRustLib")
    doLast {
        val sourceDir = file("$rootDir/CasrAdapter/target/release")
        val targetDir = file("$projectDir")

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

tasks.named("processResources") {
    dependsOn("linkRustLib")
}

tasks.named("jar") {
    dependsOn("linkRustLib")
}

tasks.named("build") {
    dependsOn("linkRustLib", "deployLocal")
}