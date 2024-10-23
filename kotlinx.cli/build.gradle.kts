plugins {
    id("org.plan.research.kotlinx-fuzz-submodule")
    kotlin("plugin.serialization") version "2.0.20"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
}
