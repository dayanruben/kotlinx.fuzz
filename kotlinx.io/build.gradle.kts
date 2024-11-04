plugins {
    id("org.plan.research.kotlinx-fuzz-submodule")
}

dependencies {
    implementation(kotlin("reflect"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-io-core:0.5.4")
}