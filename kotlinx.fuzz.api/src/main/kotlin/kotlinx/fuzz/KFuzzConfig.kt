package kotlinx.fuzz

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
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
 * @param maxSingleTargetFuzzTime - max time to fuzz a single target. Default: 1 minute
 */
interface KFuzzConfig {
    val fuzzEngine: String
    val hooks: Boolean
    val keepGoing: Int
    val instrument: List<String>
    val customHookExcludes: List<String>
    val maxSingleTargetFuzzTime: Duration
    val workDir: Path
    val dumpCoverage: Boolean
    val logLevel: String
    val jacocoReports: Set<JacocoReport>

    fun toPropertiesMap(): Map<String, String>

    companion object {
        fun fromSystemProperties(): KFuzzConfig = KFuzzConfigImpl.fromSystemProperties()

        fun fromPropertiesMap(properties: Map<String, String>): KFuzzConfig =
            KFuzzConfigImpl.fromPropertiesMap(properties)
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
        defaultValue = Defaults.HOOKS,
        toString = { it.toString() },
        fromString = { it.toBooleanStrict() },
    )
    override var keepGoing: Int by KFuzzConfigProperty(
        "kotlinx.fuzz.keepGoing",
        defaultValue = Defaults.KEEP_GOING,
        validate = { require(it > 0) { "'keepGoing' must be positive" } },
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
        defaultValue = Duration.parse(Defaults.MAX_SINGLE_TARGET_FUZZ_TIME_STRING),
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
        defaultValue = Defaults.DUMP_COVERAGE,
        toString = { it.toString() },
        fromString = { it.toBooleanStrict() },
    )
    override var logLevel: String by KFuzzConfigProperty(
        "kotlinx.fuzz.log.level",
        defaultValue = "WARN",
        validate = { require(it.uppercase() in listOf("TRACE", "INFO", "DEBUG", "WARN", "ERROR")) },
        toString = { it },
        fromString = { it },
    )
    override var jacocoReports: Set<JacocoReport> by KFuzzConfigProperty(
        "kotlinx.fuzz.jacocoReportTypes",
        defaultValue = setOf(JacocoReport.HTML),
        toString = { it.joinToString(",") },
        fromString = { it.split(",").map { JacocoReport.valueOf(it.uppercase()) }.toSet() },
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
        internal object Defaults {
            const val KEEP_GOING = 1
            const val HOOKS = true
            const val DUMP_COVERAGE = true

            // string for compatibility with annotations
            const val MAX_SINGLE_TARGET_FUZZ_TIME_STRING = "1m"
        }

        fun build(block: KFuzzConfigImpl.() -> Unit): KFuzzConfig = wrapConfigErrors {
            KFuzzConfigImpl().apply {
                block()
                assertAllSet()
                validate()
            }
        }

        internal fun fromSystemProperties(): KFuzzConfig = wrapConfigErrors {
            KFuzzConfigImpl().apply {
                configProperties().forEach { it.setFromSystemProperty() }
                assertAllSet()
                validate()
            }
        }

        internal fun fromPropertiesMap(properties: Map<String, String>): KFuzzConfigImpl = wrapConfigErrors {
            KFuzzConfigImpl().apply {
                configProperties().forEach {
                    val propertyKey = it.systemProperty
                    it.setFromString(properties[propertyKey] ?: error("map missing property $propertyKey"))
                }
                assertAllSet()
                validate()
            }
        }

        internal fun fromAnotherConfig(
            config: KFuzzConfig,
            edit: KFuzzConfigImpl.() -> Unit,
        ): KFuzzConfig = wrapConfigErrors {
            fromPropertiesMap(config.toPropertiesMap()).apply { edit() }
        }
    }
}

class ConfigurationException(
    override val message: String?,
    override val cause: Throwable? = null,
) : IllegalArgumentException()

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

    internal fun setFromString(stringValue: String) {
        assertCanSet()
        cachedValue = fromString(stringValue)
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

private inline fun <T : KFuzzConfig> wrapConfigErrors(buildConfig: () -> T): T = try {
    buildConfig()
} catch (e: Throwable) {
    if (e is ConfigurationException) {
        throw e
    } else {
        throw ConfigurationException("cannot create config", e)
    }
}
