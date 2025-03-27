pluginManagement {
    includeBuild("..")
    repositories {
        mavenCentral()
        maven(url = "https://plan-maven.apal-research.com")
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    }
}
