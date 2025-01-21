@file:Suppress("unused")

package kotlinx.fuzz

import org.gradle.api.Project

fun Project.stringProperty(name: String): String? = when {
    project.hasProperty(name) -> project.property(name).toString()
    else -> null
}

fun Project.booleanProperty(name: String): Boolean? = stringProperty(name)?.toBoolean()

fun Project.intProperty(name: String): Int? = stringProperty(name)?.toIntOrNull()
