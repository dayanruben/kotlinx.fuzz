package kotlinx.fuzz.deduplication

import kotlinx.fuzz.reproducer.CrashReproducerGenerator

fun interface ReproducerProvider {
    fun createReproducerGenerator(className: String, methodName: String): CrashReproducerGenerator?
}
