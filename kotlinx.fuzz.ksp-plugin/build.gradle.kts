import kotlinx.fuzz.configurePublishing

plugins {
    id("kotlinx.fuzz.src-module")
    id("com.google.devtools.ksp") version "2.0.21-1.0.26"
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.26")
}

configurePublishing()