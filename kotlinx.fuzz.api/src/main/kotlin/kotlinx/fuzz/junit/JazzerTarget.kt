package kotlinx.fuzz.junit

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import java.lang.invoke.MethodHandle
import java.util.concurrent.atomic.AtomicReference

object JazzerTarget {
    private val target: AtomicReference<MethodHandle> = AtomicReference()
    private val instance: AtomicReference<Any> = AtomicReference()

    fun reset(target: MethodHandle, instance: Any) {
        this.target.set(target)
        this.instance.set(instance)
    }

    fun fuzzTargetOne(data: FuzzedDataProvider) {
        target.get()(instance.get(), data)
    }
}