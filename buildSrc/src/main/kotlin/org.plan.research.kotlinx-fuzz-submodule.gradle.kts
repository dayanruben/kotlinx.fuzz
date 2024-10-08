plugins {
    kotlin("jvm")
}

group = GROUP_ID
version = VERSION

repositories {
    mavenCentral()
    maven(url = "https://maven.apal-research.com")
    maven(url = "${project.parent!!.projectDir.absolutePath}/lib")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("com.code-intelligence:jazzer-junit:$jazzerVersion")
    testImplementation("com.code-intelligence:jazzer-api:$jazzerVersion")
}

kotlin {
    jvmToolchain(17)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging.showStandardStreams = true

    // set up Jazzer options
    environment(mapOf("JAZZER_FUZZ" to "1"))
    maxHeapSize = "${1024 * 4}m"
    jvmArgs("-Xss1g")
}
