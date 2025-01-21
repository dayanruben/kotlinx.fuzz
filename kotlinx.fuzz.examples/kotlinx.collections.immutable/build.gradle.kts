plugins {
    id("kotlinx.fuzz.example-module")
}

dependencies {
    testImplementation(kotlin("reflect"))
    implementation("org.reflections:reflections:0.10.2")

    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
}
