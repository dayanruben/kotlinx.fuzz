package kotlinx.fuzz.config

/**
 * it is not possible to split config classes into different subprojects, because there will
 * have to be a single scope for editing it, and it will have to be available everywhere. So
 * KFConfigImpl must live here, and it has to access all other configs, which also makes them live here.
 * Therefore, no particular need to get those classes non-inner.
 * NO!
 * It would still be great if we could but all those configs in different files, we have a lot of them.
 */

// TODO

interface KFConfig {
    val global: GlobalConfig
    val target: TargetConfig

    fun toPropertiesMap(): Map<String, String>

    companion object {
        fun fromSystemProperties(): KFConfigBuilder = KFConfigBuilder(getSystemPropertiesMap())
        fun fromAnotherConfig(config: KFConfig): KFConfigBuilder = KFConfigBuilder(config.toPropertiesMap())

        val CONFIG_NAME_PREFIX = "kotlinx.fuzz."
    }
}

internal interface KFConfigHolder {
    val builder: KFConfigBuilder
}

internal fun <T : Any> KFConfigHolder.kfProperty(
    nameSuffix: String,
    intoString: (T) -> String,
    fromString: (String) -> T,
    validate: (T) -> Unit = {},
    default: T? = null,
): KFConfigBuilder.KFProp<T> = builder.KFProp(
    name = KFConfig.CONFIG_NAME_PREFIX + nameSuffix,
    fromString = fromString,
    intoString = intoString,
    validate = validate,
    default = default,
)
