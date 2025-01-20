plugins {
    kotlin("jvm") version "2.0.21"
    id("kotlinx.fuzz")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}
