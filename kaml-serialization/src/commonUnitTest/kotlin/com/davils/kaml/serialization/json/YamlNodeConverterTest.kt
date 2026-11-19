/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.davils.kaml.serialization.json

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.serialization.fixture.GROWTH_FACTOR
import com.davils.kaml.serialization.fixture.collidingKeys
import com.davils.kaml.serialization.fixture.fastestRun
import com.davils.kaml.serialization.fixture.shouldGrowLinearlyFrom
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Duration

private const val KEYS: Int = 1024

private fun objectOf(count: Int): JsonObject {
    val keys = collidingKeys(count)
    return JsonObject(keys.indices.associate { index -> keys[index] to JsonPrimitive(index) })
}

private fun conversionTime(count: Int): Duration {
    val element = objectOf(count)
    return fastestRun { element.toYamlNode() }
}

class YamlNodeConverterTest : FunSpec({
    context("a JSON object whose keys have colliding hash codes") {
        test("is converted with every key") {
            val node = objectOf(KEYS).toYamlNode()
            node.shouldBeInstanceOf<YamlNode.Mapping>().entries.size shouldBe KEYS
        }

        test("is converted in time that grows with its size times its logarithm") {
            val small = conversionTime(KEYS)
            val large = conversionTime(KEYS * GROWTH_FACTOR)

            large shouldGrowLinearlyFrom small
        }
    }
})
