plugins {
    id("org.plan.research.kotlinx-fuzz-module")
}

repositories {
    mavenLocal()
}

dependencies {
    implementation(project(":kotlinx.fuzz.api"))
    implementation("com.code-intelligence:jazzer:0.0.0-dev")
    implementation(kotlin("reflect"))
}

tasks.register<Exec>("deployLocal") {
    workingDir = file("$rootDir/jazzer/deploy")
    commandLine = listOf("bash", "deploy_local.sh")
}

tasks.named("build") {
    dependsOn("deployLocal")
}