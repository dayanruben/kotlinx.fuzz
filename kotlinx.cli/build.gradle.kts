plugins {
    id("org.plan.research.kotlinx-fuzz-submodule")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    implementation(kotlin("reflect"))
}
