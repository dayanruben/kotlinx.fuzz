plugins {
    id("org.plan.research.kotlinx-fuzz-module")
}

dependencies {
    implementation("com.code-intelligence:jazzer:$jazzerVersion")
    implementation(kotlin("reflect"))
}
