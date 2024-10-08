plugins {
    id("org.plan.research.kotlinx-fuzz-main")
}


tasks.register<DownloadDependency>("downloadDependency") {
    targetDirectory.set("${project.projectDir.toPath().toAbsolutePath()}/lib")
    dependencyName.set(project.providers.gradleProperty("dependencyName"))
}