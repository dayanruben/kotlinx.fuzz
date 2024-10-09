package org.plan.research

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import kotlinx.serialization.properties.encodeToStringMap
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

object PropertiesReproductionTests {
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `missing field for empty primitive array`() {
        val value: Value = BooleanArrayValue(booleanArrayOf())
        val strMap = Properties.encodeToStringMap(value)
        // Fails with
        // "kotlinx.serialization.MissingFieldException: Field 'value' is required for type with serial name 'org.plan.research.BooleanArrayValue', but it was missing"
        val decodedValue = Properties.decodeFromStringMap<Value>(strMap)
        assertTrue { value == decodedValue }
    }

    /**
     * This is kind of expected behaviour, according to the docs
     * Properties serializer omits null values
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `missing field for null value`() {
        val value: Value = CompositeNullableValue(IntValue(0), NullValue, null)
        val strMap = Properties.encodeToStringMap(value)
        val decodedValue = Properties.decodeFromStringMap<Value>(strMap)
        assertTrue { value == decodedValue }
    }
}