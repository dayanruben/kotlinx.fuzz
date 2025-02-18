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


interface KFConfig {
    val global: GlobalConfig
    val target: TargetConfig

    fun toPropertiesMap(): Map<String, String>
}

interface GlobalConfig {
    val workDir: Path
}

interface TargetConfig {
    val maxTime: Duration
}

class KFBuilder(
    private val propertiesMap: Map<String, String>,
) {
    // =========== config declarations ===========

    inner class GlobalConfigImpl internal constructor() : GlobalConfig {
        override var workDir by Prop<Path>(
            nameSuffix = "workDir",
            fromString = { Path.of(it) },
            intoString = { it.absolutePathString() },
        )
    }

    inner class TargetConfigImpl internal constructor() : TargetConfig {
        override var maxTime by Prop<Duration>(
            nameSuffix = "maxTime",
            fromString = { Duration.parse(it) },
            intoString = { it.toString() },
            validate = { require(it.isPositive()) { "maxTime must be positive!" } },
            default = { Duration.parse("1m") }
        )
    }

    // =========== builder implementation ===========

    private var isBuilt = false
    private var editOverride = false
    private val delegates = mutableListOf<Prop<*>>()

    /**
     * A fuzzing config property. The value is looked up with the following priority:
     *
     * 1) editOverride
     * 2) property map
     * 3) editFallback
     * 4) default
     */
    private inner class Prop<T : Any>(
        nameSuffix: String,
        private val fromString: (String) -> T,
        private val intoString: (T) -> String,
        private val validate: (T) -> Unit = {}, // throws
        private val default: (() -> T)? = null,
    ) {
        val name = CONFIG_NAME_PREFIX + nameSuffix

        init {
            delegates += this
        }

        private var value: T? = null

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            check(isBuilt) { "cannot get value, config is not built yet!" }
            return value!!
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            check(!isBuilt) { "cannot set value, config is already built!" }
            this.value = value
        }

        val isSet get() = value != null

        fun setFromPropertiesMap() {
            propertiesMap[name]?.let {
                value = fromString(it)
            }
        }

        fun setFromDefault() {
            default?.let {
                value = it()
            }
        }

        fun getStringValue(): String {
            check(isBuilt) { "cannot get string value, config is not built yet!" }
            return intoString(value!!)
        }

        fun validate() {
            check(value != null) { "property '$name' was not set!" }
            validate(value!!)
        }
    }

    inner class KFConfigImpl internal constructor() : KFConfig {
        override val global = GlobalConfigImpl()
        override val target = TargetConfigImpl()

        override fun toPropertiesMap(): Map<String, String> {
            check(isBuilt) { "cannot get properties map, config is not built yet!" }
            return delegates.associate { it.name to it.getStringValue() }
        }
    }

    companion object {
        fun fromSystemProperties(): KFBuilder = KFBuilder(getSystemPropertiesMap())
        fun fromAnotherConfig(config: KFConfig): KFBuilder = KFBuilder(config.toPropertiesMap())

        val CONFIG_NAME_PREFIX = "kotlinx.fuzz."
    }

    private val overrideSteps = mutableListOf<KFConfigImpl.() -> Unit>()
    private val fallbackSteps = mutableListOf<KFConfigImpl.() -> Unit>()

    fun editOverride(editor: KFConfigImpl.() -> Unit): KFBuilder = this.also {
        overrideSteps.add(editor)
    }

    fun editFallback(editor: KFConfigImpl.() -> Unit): KFBuilder = this.also {
        fallbackSteps.add(editor)
    }

    fun build(): KFConfig {
        check(!isBuilt) { "config is already built!" }
        try {
            val config = KFConfigImpl()
            /*
              To guarantee the priority in the following order...
              1) editOverride
              2) property map
              3) editFallback
              4) default
              ...we can set values in backwards order.
             */
            delegates.forEach { it.setFromDefault() }
            fallbackSteps.forEach { it.invoke(config) }
            delegates.forEach { it.setFromPropertiesMap() }
            overrideSteps.forEach { it.invoke(config) }

            isBuilt = true
            return config
        } catch (e: Throwable) {
            throw ConfigurationException(e)
        }
    }
}

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

        fun fromPropertiesMap(properties: Map<String, String>): KFuzzConfig =
            KFuzzConfigImpl.fromPropertiesMap(properties)
    }
}

class KFuzzConfigImpl private constructor() : KFuzzConfig {
    override var fuzzEngine: String by KFuzzConfigProperty(
        SystemProperty.ENGINE,
        defaultValue = "jazzer",
        fromString = { it },
        toString = { it },
    )
    override var hooks: Boolean by KFuzzConfigProperty(
        SystemProperty.HOOKS,
        defaultValue = Defaults.HOOKS,
        toString = { it.toString() },
        fromString = { it.toBooleanStrict() },
    )
    override var keepGoing: Long by KFuzzConfigProperty(
        SystemProperty.KEEP_GOING,
        defaultValue = Defaults.KEEP_GOING,
        validate = { require(it > 0) { "'keepGoing' must be positive" } },
        toString = { it.toString() },
        fromString = { it.toLong() },
    )
    override var instrument: List<String> by KFuzzConfigProperty(
        SystemProperty.INSTRUMENT,
        toString = { it.joinToString(",") },
        fromString = { it.split(",") },
    )
    override var customHookExcludes: List<String> by KFuzzConfigProperty(
        SystemProperty.CUSTOM_HOOK_EXCLUDES,
        defaultValue = emptyList(),
        toString = { it.joinToString(",") },
        fromString = { it.split(",") },
    )
    override var maxSingleTargetFuzzTime: Duration by KFuzzConfigProperty(
        SystemProperty.MAX_SINGLE_TARGET_FUZZ_TIME,
        defaultValue = Duration.parse(Defaults.MAX_SINGLE_TARGET_FUZZ_TIME_STRING),
        validate = { require(it.inWholeSeconds > 0) { "'maxSingleTargetFuzzTime' must be at least 1 second" } },
        toString = { it.inWholeSeconds.toString() },
        fromString = { it.toInt().seconds },
    )
    override var workDir: Path by KFuzzConfigProperty(
        SystemProperty.WORK_DIR,
        toString = { it.toString() },
        fromString = { Path(it).absolute() },
    )
    override var dumpCoverage: Boolean by KFuzzConfigProperty(
        SystemProperty.DUMP_COVERAGE,
        defaultValue = Defaults.DUMP_COVERAGE,
        toString = { it.toString() },
        fromString = { it.toBooleanStrict() },
    )
    override var reproducerPath: Path by KFuzzConfigProperty(
        SystemProperty.REPRODUCER_PATH,
        toString = { it.absolutePathString() },
        fromString = { Path(it).absolute() },
    )
    override var logLevel: String by KFuzzConfigProperty(
        SystemProperty.LOG_LEVEL,
        defaultValue = "WARN",
        validate = { require(it.uppercase() in listOf("TRACE", "INFO", "DEBUG", "WARN", "ERROR")) },
        toString = { it },
        fromString = { it },
    )

    override fun toPropertiesMap(): Map<String, String> = configProperties()
        .associate { it.systemProperty.name to it.stringValue }

    private fun assertAllSet() {
        configProperties().forEach { it.assertIsSet() }
    }

    private fun validate() {
        configProperties().forEach { it.validate() }
    }

    companion object {
        internal object Defaults {
            const val KEEP_GOING = 1L
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
                    val propertyKey = it.systemProperty.name
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

class ConfigurationException(cause: Throwable?) : IllegalArgumentException("cannot create config", cause)

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
    val systemProperty: SystemProperty,
    val defaultValue: T? = null,
    private val validate: (T) -> Unit = {},
    private val toString: (T) -> String,
    private val fromString: (String) -> T,
) : ReadWriteProperty<Any, T> {
    private val name: String = systemProperty.name.substringAfterLast('.')
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
        systemProperty.get()?.let {
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
    throw when (e) {
        is ConfigurationException -> e
        else -> ConfigurationException(e)
    }
}

fun getSystemPropertiesMap(): Map<String, String> = buildMap {
    val properties = System.getProperties()
    val propNames = properties.propertyNames()
    for (name in propNames) {
        val key = name.toString()
        val value = properties.getProperty(key).toString()
        put(key, value)
    }
}
