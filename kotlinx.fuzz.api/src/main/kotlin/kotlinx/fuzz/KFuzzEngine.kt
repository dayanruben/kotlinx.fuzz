package kotlinx.fuzz

import java.lang.reflect.Method

interface KFuzzEngine {
    fun initialise(config: Config)
    fun runTarget(instance: Any, method: Method): Throwable?
}