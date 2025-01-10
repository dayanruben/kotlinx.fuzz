plugins {
    id("org.plan.research.kotlinx-fuzz-module")
}

dependencies {
    implementation("com.github.curious-odd-man:rgxgen:2.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}
