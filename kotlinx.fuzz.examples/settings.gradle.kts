pluginManagement {
    includeBuild("..")
    repositories {
        mavenCentral()
        maven(url = "https://plan-maven.apal-research.com")
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

include("kotlinx.serialization")
include("kotlinx.html")
include("kotlinx.io")
include("kotlinx.cli")
include("kotlinx.collections.immutable")
include("kotlinx.datetime")
