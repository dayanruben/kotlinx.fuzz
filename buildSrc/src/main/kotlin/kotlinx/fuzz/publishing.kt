package kotlinx.fuzz

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.get

fun Project.configurePublishing() {
    pluginManager.apply("maven-publish")
    project.pluginManager.withPlugin("maven-publish") {
        project.afterEvaluate {
            project.extensions.configure<PublishingExtension>("publishing") {
                publications {
                    this@publications.create("runner", MavenPublication::class.java) {
                        groupId = project.group.toString()
                        artifactId = project.name
                        version = project.version.toString()

                        from(components["java"])
                    }
                }
                repositories {
                    maven {
                        url = uri("https://maven.pkg.github.com/plan-research/kotlin-maven")
                        credentials {
                            username = project.findProperty("gpr.user")?.toString()
                                ?: System.getenv("MAVEN_REPOSITORY_LOGIN")
                            password = project.findProperty("gpr.token")?.toString()
                                ?: System.getenv("MAVEN_REPOSITORY_PASSWORD")
                        }
                    }
                }
            }
        }
    }
}
