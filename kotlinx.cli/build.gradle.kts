plugins {
    id("org.plan.research.kotlinx-fuzz-submodule")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    implementation("commons-cli:commons-cli:1.9.0")
    implementation(kotlin("reflect"))
}
