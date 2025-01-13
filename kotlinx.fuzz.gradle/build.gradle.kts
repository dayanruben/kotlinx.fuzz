plugins {
    id("org.plan.research.kotlinx-fuzz-module")
    `kotlin-dsl`
}

dependencies {
    implementation(project(":kotlinx.fuzz.api"))
    implementation(project(":kotlinx.fuzz.jazzer"))

    gradleApi()
    implementation(kotlin("reflect"))
    implementation("org.junit.platform:junit-platform-engine:$junitPlatformVersion")

    testImplementation("org.junit.platform:junit-platform-testkit:$junitPlatformVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("com.code-intelligence:jazzer-api:$jazzerVersion")
}

gradlePlugin.plugins.create("kotlinx.fuzz") {
    id = "kotlinx.fuzz-gradle"
    implementationClass = "org.planx.fuzzing.plugin.KFuzzPlugin"
}

tasks.test {
    useJUnitPlatform {
        excludeEngines("kotlinx.fuzz")
    }
}