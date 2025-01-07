plugins {
    id("org.plan.research.kotlinx-fuzz-example")
}

dependencies {
    testImplementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    implementation(kotlin("reflect"))
    implementation("org.reflections:reflections:0.10.2")
}
