plugins {
    kotlin("jvm") version "2.0.10"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")

    val jazzerVersion = "0.21.1"
    testImplementation("com.code-intelligence:jazzer-api:$jazzerVersion")
    testImplementation("com.code-intelligence:jazzer-junit:$jazzerVersion")

    testImplementation(kotlin("reflect"))
    implementation("org.reflections:reflections:0.10.2")

    val jsoupVersion = "1.18.1"
    // For parsing HTML
    implementation("org.jsoup:jsoup:$jsoupVersion")

    val kotlinxHtmlVersion = "0.11.0"
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinxHtmlVersion")
}

tasks.test {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
    maxHeapSize = "${1024 * 4}m"
}

kotlin {
    jvmToolchain(17)
}
