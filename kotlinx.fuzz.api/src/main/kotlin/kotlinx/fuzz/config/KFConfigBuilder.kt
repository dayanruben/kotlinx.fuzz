package kotlinx.fuzz.config

import kotlinx.fuzz.*
import kotlin.reflect.KProperty

class KFConfigBuilder(
    private val propertiesMap: Map<String, String>,
) {
    private var isBuilt = false
    // FQN --> KFProp
    private val delegatesMap = mutableMapOf<String, KFProp<*>>()

    private val overrideSteps = mutableListOf<KFConfigImpl.() -> Unit>()
    private val fallbackSteps = mutableListOf<KFConfigImpl.() -> Unit>()

    private val configImpl = KFConfigImpl()

    inner class KFPropProvider<T : Any>(
        private val nameSuffix: String,
        private val intoString: (T) -> String,
        private val fromString: (String) -> T,
        private val validate: (T) -> Unit = {},
        private val default: T? = null,
    ) {
        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): KFProp<T> {
            val kfProp = KFProp(
                name = KFConfig.CONFIG_NAME_PREFIX + nameSuffix,
                fromString = fromString,
                intoString = intoString,
                validate = validate,
                default = default,
            )
            delegatesMap[property.toString()] = kfProp
            println("put for $property obj ${property.hashCode()}")
            return kfProp
        }
    }

    inner class KFProp<T : Any>(
        val name: String,
        private val fromString: (String) -> T,
        private val intoString: (T) -> String,
        private val validate: (T) -> Unit, // throws
        private val default: T?,
    ) {
        private var value: T? = null
        private val isBuilt get() = this@KFConfigBuilder.isBuilt

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            check(value != null) { "property '${name}' is not set yet" }
            return value!!
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            check(!isBuilt) { "cannot set value, config is already built!" }
            this.value = value
        }

        fun setFromPropertiesMap() {
            propertiesMap[name]?.let {
                value = fromString(it)
            }
        }

        fun setFromDefault() {
            default?.let {
                value = it
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
        override val global = GlobalConfigImpl(this@KFConfigBuilder)
        override val target = TargetConfigImpl(this@KFConfigBuilder)

        // TODO: we have to know which engine to use before building the config...
        // Viable solution for future: build a stub config, check the engine, build a new config.
        // Will need to be careful with instantiating delegates, as they will get validated.
        override val engine = JazzerConfigImpl(this@KFConfigBuilder)

        override fun toPropertiesMap(): Map<String, String> {
            check(isBuilt) { "cannot get properties map, config is not built yet!" }
            return delegatesMap.values.associate { it.name to it.getStringValue() }
        }
    }

    fun editOverride(editor: KFConfigImpl.() -> Unit): KFConfigBuilder = this.also {
        overrideSteps.add(editor)
    }

    fun editFallback(editor: KFConfigImpl.() -> Unit): KFConfigBuilder = this.also {
        fallbackSteps.add(editor)
    }

    fun build(): KFConfig {
        check(!isBuilt) { "config is already built!" }
        try {
            /*
              To guarantee the priority in the following order...
              1) editOverride
              2) property map
              3) editFallback
              4) default
              ...we can set values in backwards order.
             */
            val delegates = delegatesMap.values
            delegates.forEach { it.setFromDefault() }
            fallbackSteps.forEach { it.invoke(configImpl) }
            delegates.forEach { it.setFromPropertiesMap() }
            overrideSteps.forEach { it.invoke(configImpl) }

            delegates.forEach { it.validate() }
            isBuilt = true
            return configImpl
        } catch (e: Throwable) {
            throw ConfigurationException(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getPropertyDelegate(propertySelector: KFConfigImpl.() -> KProperty<T>): KFProp<T> {
        val property = propertySelector(configImpl)

        val kfProp = delegatesMap[property.toString()] ?: error("no KFProp found for property '${property.name}'")
        return kfProp as KFProp<T>
    }
}

/**
 * A fuzzing config property. The value is looked up with the following priority:
 *
 * 1) editOverride
 * 2) property map
 * 3) editFallback
 * 4) default
 */

fun getSystemPropertiesMap(): Map<String, String> = buildMap {
    val properties = System.getProperties()
    val propNames = properties.propertyNames()
    for (name in propNames) {
        val key = name.toString()
        val value = properties.getProperty(key).toString()
        put(key, value)
    }
}