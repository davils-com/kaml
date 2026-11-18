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

package com.davils.kaml.serialization.decode

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.serialization.fixture.GROWTH_FACTOR
import com.davils.kaml.serialization.fixture.Port
import com.davils.kaml.serialization.fixture.collidingKeys
import com.davils.kaml.serialization.fixture.fastestRun
import com.davils.kaml.serialization.fixture.shouldGrowLinearlyFrom
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.time.Duration

private const val KEYS: Int = 1024
private const val PORT: Int = 80
private const val FIRST: Int = 1
private const val SECOND: Int = 2
private const val THIRD: Int = 3
private const val FIFTH: Int = 5
private const val ZERO: Double = 0.0

private fun repeatedOf(vararg values: Any?): List<YamlNode> = repeatedIn(values.toList())

private fun repeatedIn(values: List<Any?>): List<YamlNode> {
    val keys = DecodedKeys()
    values.forEachIndexed { index, value -> keys.add(value, yamlScalar(index)) }
    return keys.repeated()
}

private fun at(vararg positions: Int): List<YamlNode> = positions.map { position -> yamlScalar(position) }

private fun detectionTime(count: Int): Duration {
    val names = collidingKeys(count)
    val keys = DecodedKeys()
    names.forEachIndexed { index, name -> keys.add(name, yamlScalar(index)) }
    return fastestRun { keys.repeated().shouldBeEmpty() }
}

class DecodedKeysTest : FunSpec({
    context("keys that decode to equal values") {
        test("are repeats after their first occurrence, in the order of the mapping") {
            repeatedOf("a", "b", "a", "a") shouldBe at(SECOND, THIRD)
            repeatedOf(1, 2L, 1, 2L) shouldBe at(SECOND, THIRD)
            repeatedOf(null, "x", null) shouldBe at(SECOND)
            repeatedOf(Double.NaN, Double.NaN) shouldBe at(FIRST)
            repeatedOf(Port(PORT), Port(PORT + 1), Port(PORT)) shouldBe at(SECOND)
            repeatedOf(1u, 1u, 'c', 'c', true, true) shouldBe at(FIRST, THIRD, FIFTH)
        }

        test("are told apart from values that are only alike") {
            repeatedOf(1, 1L, 1.0, "1", '1', 1u).shouldBeEmpty()
            repeatedOf(ZERO, -ZERO).shouldBeEmpty()
            repeatedIn(collidingKeys(KEYS)).shouldBeEmpty()
        }
    }

    context("keys whose hash codes collide") {
        test("are checked in time that grows with their number times its logarithm") {
            val small = detectionTime(KEYS)
            val large = detectionTime(KEYS * GROWTH_FACTOR)

            large shouldGrowLinearlyFrom small
        }
    }
})
