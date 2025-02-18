package kotlinx.fuzz.config

import kotlinx.fuzz.*
import kotlin.reflect.KProperty

class KFConfigBuilder(
    private val propertiesMap: Map<String, String>,
) {
    // =========== config declarations ===========

    // =========== builder implementation ===========

    private var isBuilt = false
    private val delegates = mutableListOf<KFProp<*>>()

    internal inner class KFProp<T : Any>(
        val name: String,
        private val fromString: (String) -> T,
        private val intoString: (T) -> String,
        private val validate: (T) -> Unit, // throws
        private val default: T?,
    ) {
        init {
            delegates += this
        }

        private var value: T? = null
//        private val isBuilt get() = this@KFConfigBuilder.isBuilt

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            check(isBuilt) { "cannot get value, config is not built yet!" }
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

        override fun toPropertiesMap(): Map<String, String> {
            check(isBuilt) { "cannot get properties map, config is not built yet!" }
            return delegates.associate { it.name to it.getStringValue() }
        }
    }

    private val overrideSteps = mutableListOf<KFConfigImpl.() -> Unit>()
    private val fallbackSteps = mutableListOf<KFConfigImpl.() -> Unit>()

    fun editOverride(editor: KFConfigImpl.() -> Unit): KFConfigBuilder = this.also {
        overrideSteps.add(editor)
    }

    fun editFallback(editor: KFConfigImpl.() -> Unit): KFConfigBuilder = this.also {
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

            delegates.forEach { it.validate() }
            isBuilt = true
            return config
        } catch (e: Throwable) {
            throw ConfigurationException(e)
        }
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