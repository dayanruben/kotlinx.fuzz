package kotlinx.fuzz

import com.saveourtool.diktat.plugin.gradle.DiktatExtension
import com.saveourtool.diktat.plugin.gradle.DiktatGradlePlugin
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

fun Project.configureDiktat(excludePredicate: (FileTreeElement) -> Boolean = { false }) {
    apply<DiktatGradlePlugin>()
    configure<DiktatExtension> {
        githubActions = true
        diktatConfigFile = rootProject.file("diktat-analysis.yml")
        reporters {
            plain()
            sarif()
        }
        inputs {
            exclude { excludePredicate(it) }
            include("src/**/*.kt")
        }
        debug = true
    }
}
