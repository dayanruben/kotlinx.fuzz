package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import kotlinx.serialization.properties.encodeToStringMap
import kotlin.test.assertTrue

object PropertiesTest {
    private fun isMissingFieldException(e: Throwable): Boolean =
        e.javaClass.name == "kotlinx.serialization.MissingFieldException"

    @OptIn(ExperimentalSerializationApi::class)
    @FuzzTest(maxDuration = TEST_DURATION)
    fun propertiesEncodeAndDecode(dataProvider: FuzzedDataProvider) {
        val value = dataProvider.generateValue(MAX_STR_LENGTH)
        val decodedValue: Value
        try {
            val strMap = Properties.encodeToStringMap<Value>(value)
            decodedValue = Properties.decodeFromStringMap(strMap)
            assertTrue { value == decodedValue }
        } catch (e: Throwable) {
            if (isMissingFieldException(e)) {
                return
            } else {
                throw e
            }
        }
    }
}