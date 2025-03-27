plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven(url = "https://plan-maven.apal-research.com")
    maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
}

dependencies {
    testImplementation(kotlin("test"))
}


kotlin {
    jvmToolchain(8)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
}
