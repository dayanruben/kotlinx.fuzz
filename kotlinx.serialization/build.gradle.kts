plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("com.code-intelligence:jazzer-junit:0.22.1")
    testImplementation("com.code-intelligence:jazzer-api:0.22.1")


    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.7.3")
}

kotlin {
    jvmToolchain(17)
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
    testLogging.showStandardStreams = true // adds more info to logs

    maxHeapSize = "${1024 * 4}m"
    jvmArgs("-Xss1g")
}

task("copyDependencies", Copy::class) {
    from(configurations.runtimeClasspath).into("${project.layout.buildDirectory.get()}/dependencies")
}

tasks.getByName("test").dependsOn("copyDependencies")