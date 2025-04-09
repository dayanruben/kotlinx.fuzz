package kotlinx.fuzz.jazzer

import com.code_intelligence.jazzer.api.MethodHook
import com.code_intelligence.jazzer.api.MethodHooks
import java.lang.reflect.Method
import java.nio.file.FileSystems
import kotlin.io.path.Path
import kotlinx.fuzz.config.KFuzzConfig
import org.reflections.Reflections
import org.reflections.scanners.Scanners.MethodsAnnotated
import org.reflections.util.ConfigurationBuilder

object CustomHooks {
    fun findCustomHookClasses(config: KFuzzConfig): Set<Class<*>> {
        val reflections = Reflections(ConfigurationBuilder().apply {
            forPackage("")
            scanners += MethodsAnnotated
        })
        val excludedClassesMatchers = config.global.customHookExcludes.map {
            FileSystems.getDefault().getPathMatcher("glob:$it")
        }

        fun isExcluded(hookClass: Class<*>) = excludedClassesMatchers.any {
            it.matches(Path("", hookClass.name))
        }

        val hookMethods = reflections.get(
            MethodsAnnotated
                .with(MethodHook::class.java, MethodHooks::class.java)
                .`as`(Method::class.java)
                .filter {
                    val hookClass = it.declaringClass
                    val classPackage = hookClass.`package`?.name ?: return@filter true
                    when {
                        classPackage.startsWith("com.code_intelligence.jazzer") -> false
                        isExcluded(hookClass) -> false
                        else -> true
                    }
                })

        return buildSet {
            for (hook in hookMethods) {
                add(hook.declaringClass)
            }
        }
    }
}
