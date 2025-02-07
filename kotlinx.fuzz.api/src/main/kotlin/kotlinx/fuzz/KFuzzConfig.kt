package kotlinx.fuzz

import java.nio.file.Path
import kotlin.io.path.*
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
 * @param hooks - apply fuzzing instrumentation. Default: true
 * @param keepGoing - how many bugs to discover before finishing fuzzing. Default: 1
 * @param instrument - glob patterns matching names of classes that should be instrumented for fuzzing
 * @param customHookExcludes - Glob patterns matching names of classes that should not be instrumented with hooks
 * @param workDir - Directory where the all fuzzing results will be stored. Default: `build/fuzz`
 * @param dumpCoverage - Whether fuzzer will generate jacoco .exec files.
 * @param logLevel - Logging level enabled for kotlinx.fuzz
 * Default: true
 * (custom and built-in).
 * Default: empty list
 * @param maxSingleTargetFuzzTime - max time to fuzz a single target in seconds
 * @param reproducerPath - Path to store reproducers. Default: `$workDir/reproducers`
 */
interface KFuzzConfig {
    val fuzzEngine: String
    val hooks: Boolean
    val keepGoing: Long
    val instrument: List<String>
    val customHookExcludes: List<String>
    val maxSingleTargetFuzzTime: Duration
    val workDir: Path
    val dumpCoverage: Boolean
    val reproducerPath: Path
    val logLevel: String

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
        defaultValue = true,
        toString = { it.toString() },
        fromString = { it.toBooleanStrict() },
    )
    override var keepGoing: Long by KFuzzConfigProperty(
        "kotlinx.fuzz.keepGoing",
        defaultValue = 1,
        validate = { require(it > 0) { "'keepGoing' must be positive" } },
        toString = { it.toString() },
        fromString = { it.toLong() },
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
        validate = { require(it.inWholeSeconds > 0) { "'maxSingleTargetFuzzTime' must be at least 1 second" } },
        toString = { it.inWholeSeconds.toString() },
        fromString = { it.toInt().seconds },
    )
    override var workDir: Path by KFuzzConfigProperty(
        "kotlinx.fuzz.workDir",
        toString = { it.toString() },
        fromString = { Path(it).absolute() },
    )
    override var dumpCoverage: Boolean by KFuzzConfigProperty(
        "kotlinx.fuzz.dumpCoverage",
        defaultValue = true,
        toString = { it.toString() },
        fromString = { it.toBooleanStrict() },
    )
    override var reproducerPath: Path by KFuzzConfigProperty(
        "kotlinx.fuzz.reproducerPath",
        toString = { it.absolutePathString() },
        fromString = { Path(it).absolute() },
    )
    override var logLevel: String by KFuzzConfigProperty(
        "kotlinx.fuzz.log.level",
        defaultValue = "WARN",
        validate = { require(it.uppercase() in listOf("TRACE", "INFO", "DEBUG", "WARN", "ERROR")) },
        toString = { it },
        fromString = { it },
    )

    override fun toPropertiesMap(): Map<String, String> = configProperties()
        .associate { it.systemProperty to it.stringValue }

    private fun assertAllSet() {
        configProperties().forEach { it.assertIsSet() }
    }

    private fun validate() {
        configProperties().forEach { it.validate() }
    }

    companion object {
        fun build(block: KFuzzConfigImpl.() -> Unit): KFuzzConfig = KFuzzConfigImpl().apply {
            block()
            assertAllSet()
            validate()
        }

        internal fun fromSystemProperties(): KFuzzConfig = KFuzzConfigImpl().apply {
            configProperties().forEach { it.setFromSystemProperty() }
            assertAllSet()
            validate()
        }
    }
}

/**
 * A delegate property class that manages a configuration option for KFuzz.
 *
 * Can be set only once per instance.
 * `KFuzzConfigImpl.build` set it from `systemProperty`
 *
 * @param systemProperty the system property key associated with this configuration option
 * @param defaultValue the default value for this configuration, if none is provided
 * @param validate a function which asserts if the value is correct
 * @param toString a function that converts the property value to its string representation
 * @param fromString a function that converts a string value to the property value type
 */
internal class KFuzzConfigProperty<T : Any> internal constructor(
    val systemProperty: String,
    val defaultValue: T? = null,
    private val validate: (T) -> Unit = {},
    private val toString: (T) -> String,
    private val fromString: (String) -> T,
) : ReadWriteProperty<Any, T> {
    private val name: String = systemProperty.substringAfterLast('.')
    private var cachedValue: T? = null
    val stringValue: String get() = toString(get())

    override fun getValue(thisRef: Any, property: KProperty<*>): T = get()

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        cachedValue = value
    }

    internal fun get(): T {
        cachedValue ?: defaultValue?.let { cachedValue = it } ?: error("Option '$name' is not set")
        return cachedValue!!
    }

    internal fun validate(): Unit = validate(get())

    internal fun assertIsSet() {
        get()
    }

    internal fun setFromSystemProperty() {
        assertCanSet()
        System.getProperty(systemProperty)?.let {
            cachedValue = fromString(it)
        } ?: error("System property '$systemProperty' is not set")
    }

    private fun assertCanSet() {
        cachedValue?.let {
            error("Property '$name' is already set")
        }
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
