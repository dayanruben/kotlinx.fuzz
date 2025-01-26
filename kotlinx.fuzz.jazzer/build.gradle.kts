plugins {
    id("kotlinx.fuzz.src-module")
}

repositories {
    mavenLocal()
}

dependencies {
    implementation(project(":kotlinx.fuzz.api"))
    implementation(kotlin("reflect"))
    implementation(libs.jazzer)
}

tasks.register<Exec>("deployLocal") {
    workingDir = file("$rootDir/jazzer/deploy")
    commandLine = listOf("bash", "deploy_local.sh")
}

tasks.named("build") {
    dependsOn("deployLocal")
}