plugins {
    id("org.plan.research.kotlinx-fuzz-module")
    `kotlin-dsl`
}

dependencies {
    gradleApi()
}

gradlePlugin.plugins.create("kotlinx.fuzz") {
    id = "kotlinx.fuzz-gradle"
    implementationClass = "org.planx.fuzzing.plugin.KFuzzPlugin"
}
