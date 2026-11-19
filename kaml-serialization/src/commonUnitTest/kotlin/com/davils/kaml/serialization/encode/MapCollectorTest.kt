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
package com.davils.kaml.serialization.encode

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.serialization.YamlFormat
import com.davils.kaml.serialization.fixture.GROWTH_FACTOR
import com.davils.kaml.serialization.fixture.collidingKeys
import com.davils.kaml.serialization.fixture.fastestRun
import com.davils.kaml.serialization.fixture.shouldGrowLinearlyFrom
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.time.Duration

private const val KEYS: Int = 1024

private val serializer = MapSerializer(String.serializer(), Int.serializer())

private fun mapOf(count: Int): Map<String, Int> {
    val keys = collidingKeys(count)
    return keys.indices.associate { index -> keys[index] to index }
}

private fun encodingTime(count: Int): Duration {
    val map = mapOf(count)
    return fastestRun { YamlFormat.Default.encodeToNode(serializer, map) }
}

class MapCollectorTest : FunSpec({
    context("a map whose keys have colliding hash codes") {
        test("is encoded with every key") {
            val node = YamlFormat.Default.encodeToNode(serializer, mapOf(KEYS))
            node.shouldBeInstanceOf<YamlNode.Mapping>().entries.size shouldBe KEYS
        }

        test("is encoded in time that grows with its size times its logarithm") {
            val small = encodingTime(KEYS)
            val large = encodingTime(KEYS * GROWTH_FACTOR)

            large shouldGrowLinearlyFrom small
        }
    }
})
