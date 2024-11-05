plugins {
    id("org.plan.research.kotlinx-fuzz-submodule")
}


dependencies {
    testImplementation(kotlin("reflect"))
    implementation("org.reflections:reflections:0.10.2")

    val jsoupVersion = "1.18.1"
    // For parsing HTML
    implementation("org.jsoup:jsoup:$jsoupVersion")

    val kotlinxHtmlVersion = "0.11.0"
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinxHtmlVersion")
}
