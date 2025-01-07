plugins {
    id("org.plan.research.kotlinx-fuzz-example")
}

dependencies {
    testImplementation(kotlin("reflect"))
    implementation("org.reflections:reflections:0.10.2")

    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
}
