package kotlinx.fuzz

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Class that stores generals fuzzing configuration
 *
 * @param fuzzEngine - name of engine to be used. Default: "jazzer"
 * @param hooks - apply fuzzing instrumentation
 * @param keepGoing - how many bugs to discover before finishing fuzzing. Default: 1
 * @param instrument - glob patterns matching names of classes that should be instrumented for fuzzing
 * @param customHookExcludes - Glob patterns matching names of classes that should not be instrumented with hooks
 * (custom and built-in).
 * Default: empty list
 * @param maxSingleTargetFuzzTime - max time to fuzz a single target in seconds
 */
interface KFuzzConfig {
    val fuzzEngine: String
    val hooks: Boolean
    val keepGoing: Int
    val instrument: List<String>
    val customHookExcludes: List<String>
    val maxSingleTargetFuzzTime: Duration

    fun toPropertiesMap(): Map<String, String>

    companion object {
        fun fromSystemProperties(): KFuzzConfig = KFuzzConfigImpl.fromSystemProperties()
    }
}

class KFuzzConfigImpl private constructor() : KFuzzConfig {
    override var fuzzEngine: String by KFuzzConfigProperty(
        "kotlinx.fuzz.engine",
        defaultValue = "jazzer",
        fromString = { it },
        toString = { it },
    )
    override var hooks: Boolean by KFuzzConfigProperty(
        "kotlinx.fuzz.hooks",
        defaultValue = false,
        toString = { it.toString() },
        fromString = { it.toBooleanStrict() },
    )
    override var keepGoing: Int by KFuzzConfigProperty(
        "kotlinx.fuzz.keepGoing",
        defaultValue = 1,
        toString = { it.toString() },
        fromString = { it.toInt() },
    )
    override var instrument: List<String> by KFuzzConfigProperty(
        "kotlinx.fuzz.instrument",
        toString = { it.joinToString(",") },
        fromString = { it.split(",") },
    )
    override var customHookExcludes: List<String> by KFuzzConfigProperty(
        "kotlinx.fuzz.customHookExcludes",
        defaultValue = emptyList(),
        toString = { it.joinToString(",") },
        fromString = { it.split(",") },
    )
    override var maxSingleTargetFuzzTime: Duration by KFuzzConfigProperty(
        "kotlinx.fuzz.maxSingleTargetFuzzTime",
        toString = { it.inWholeSeconds.toString() },
        fromString = { it.toInt().seconds },
    )

    override fun toPropertiesMap(): Map<String, String> = configProperties()
        .associate { it.systemProperty to it.stringValue }

    private fun assertAllSet() {
        configProperties().forEach { it.assertIsSet() }
    }

    private fun validate() {
        require(maxSingleTargetFuzzTime.inWholeSeconds > 0) { "'maxSingleTargetFuzzTime' must be at least 1 second" }
        require(keepGoing > 0) { "'keepGoing' must be positive" }
    }

    companion object {
        fun build(block: KFuzzConfigImpl.() -> Unit): KFuzzConfig = KFuzzConfigImpl().apply {
            block()
            assertAllSet()
            validate()
        }

        internal fun fromSystemProperties(): KFuzzConfig = KFuzzConfigImpl().apply {
            validate()
        }
    }
}

internal class KFuzzConfigProperty<T : Any>(
    val systemProperty: String,
    val defaultValue: T? = null,
    val toString: (T) -> String,
    val fromString: (String) -> T,
) : ReadWriteProperty<Any, T> {
    private var cachedValue: T? = null
    val stringValue: String get() = toString(get())
    override fun getValue(thisRef: Any, property: KProperty<*>): T = get()

    internal fun get(): T {
        cachedValue?.let {
            return cachedValue!!
        }

        cachedValue = System.getProperty(systemProperty)?.let(fromString) ?: defaultValue
            ?: error("No value for property '$systemProperty'")

        return cachedValue!!
    }

    internal fun assertIsSet() {
        require(cachedValue != null || defaultValue != null) { "property '$systemProperty' is not set" }
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        cachedValue?.let {
            error("Property '${property.name}' is already set")
        }
        cachedValue = value
    }
}

private fun KProperty1<KFuzzConfigImpl, *>.asKFuzzConfigProperty(delegate: KFuzzConfigImpl): KFuzzConfigProperty<*> {
    this.isAccessible = true
    return this.getDelegate(delegate)!! as KFuzzConfigProperty<*>
}

@Suppress("TYPE_ALIAS")
private fun KFuzzConfigImpl.configProperties(): List<KFuzzConfigProperty<*>> =
    KFuzzConfigImpl::class.memberProperties
        .map { it.asKFuzzConfigProperty(this) }
