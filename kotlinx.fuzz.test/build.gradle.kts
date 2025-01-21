plugins {
    kotlin("jvm") version "2.0.21"
    id("kotlinx.fuzz")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.platform:junit-platform-engine:1.11.4")
    testImplementation("org.junit.platform:junit-platform-launcher:1.11.4")
//    testImplementation("kotlinx.fuzz:kotlinx.fuzz.gradle:0.0.1")
}

konfTest {
    a = 10
    b = "adsf"
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}