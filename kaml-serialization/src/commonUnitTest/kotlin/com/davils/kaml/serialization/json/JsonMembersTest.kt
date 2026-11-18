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

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.serialization.fixture.GROWTH_FACTOR
import com.davils.kaml.serialization.fixture.collidingKeys
import com.davils.kaml.serialization.fixture.fastestRun
import com.davils.kaml.serialization.fixture.shouldGrowLinearlyFrom
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Duration

private const val KEYS: Int = 512

private val names: List<String> = listOf("b", "a", "c")
private val elements: List<JsonElement> = names.map(::JsonPrimitive)

private fun collidingMapping(count: Int): YamlNode {
    val entries = collidingKeys(count).map { key -> YamlEntry(key = yamlScalar(key), value = yamlScalar(1)) }
    return yamlMapping(entries)
}

private fun conversionTime(count: Int): Duration {
    val node = collidingMapping(count)
    return fastestRun { node.toJsonElement().shouldBeInstanceOf<YamlJsonConversion.Converted>() }
}

class JsonMembersTest : FunSpec({
    context("the members of a converted object") {
        test("keep the order of the mapping and find every name") {
            val members = JsonMembers(names, elements)

            members.keys.toList() shouldBe names
            members["a"] shouldBe JsonPrimitive("a")
            members["d"] shouldBe null
            members.containsKey("c") shouldBe true
            members.containsKey("") shouldBe false
            members.size shouldBe names.size
        }

        test("equal a map with the same members") {
            val members = JsonMembers(names, elements)
            val expected = names.zip(elements).toMap()

            members shouldBe expected
            expected shouldBe members
            members.hashCode() shouldBe expected.hashCode()
            JsonObject(members) shouldBe JsonObject(expected)
            members.entries.first().toString() shouldBe "b=\"b\""
        }

        test("find the first name that repeats an earlier one") {
            JsonMembers.firstRepeat(listOf("a", "b", "c")) shouldBe -1
            JsonMembers.firstRepeat(listOf("a", "b", "b", "a")) shouldBe 2
            JsonMembers.firstRepeat(listOf("a", null, "b", null, "a")) shouldBe names.size + 1
        }
    }

    context("a mapping whose keys have colliding hash codes") {
        test("converts in time that grows with its size times its logarithm") {
            val small = conversionTime(KEYS)
            val large = conversionTime(KEYS * GROWTH_FACTOR)

            large shouldGrowLinearlyFrom small
        }
    }
})
