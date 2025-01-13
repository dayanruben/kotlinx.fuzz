plugins {
    id("org.plan.research.kotlinx-fuzz-module")
}

dependencies {
    implementation("com.code-intelligence:jazzer:$JAZZER_VERSION")
    implementation(kotlin("reflect"))
}
