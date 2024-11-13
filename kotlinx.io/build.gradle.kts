plugins {
    id("org.plan.research.kotlinx-fuzz-submodule")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("org.reflections:reflections:0.10.2")

    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.5.4")
}