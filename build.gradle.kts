import kotlinx.fuzz.configureDiktat

plugins {
    id("kotlinx.fuzz.src-module")
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    configureDiktat {
        val examplesDir = project(":kotlinx.fuzz.examples").projectDir.toPath().normalize()
        it.file.toPath().startsWith(examplesDir)
    }
}
