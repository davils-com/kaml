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

package com.davils.kaml.core.internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.pair
import io.kotest.property.checkAll

private const val MOST_STEPS: Int = 200
private const val NAME_RANGE: Int = 30
private const val REMOVAL: Int = 0
private const val THIRD: Int = 3

private fun nameOf(number: Int): String = "n$number"

class NameMapTest : FunSpec({
    test("stores, replaces and finds values by name") {
        val map = NameMap<Int>()
        map["b"] = 1
        map["a"] = 2
        map["b"] = THIRD

        map["b"] shouldBe THIRD
        map["a"] shouldBe 2
        map["c"].shouldBeNull()
        ("a" in map) shouldBe true
        ("c" in map) shouldBe false
        map.size shouldBe 2
    }

    test("lists the names and entries in the order they first came") {
        val map = NameMap<Int>()
        map["b"] = 1
        map["a"] = 2
        map["b"] = THIRD

        map.names shouldContainExactly listOf("b", "a")
        map.entries shouldContainExactly listOf("b" to THIRD, "a" to 2)
    }

    test("puts a value only when the name has none") {
        val map = NameMap<Int>()

        map.getOrPut("a") { 1 } shouldBe 1
        map.getOrPut("a") { 2 } shouldBe 1
        map.size shouldBe 1
    }

    test("removes a name and takes it again") {
        val map = NameMap<Int>()
        map["a"] = 1
        map["b"] = 2
        map.remove("a")
        map.remove("a")
        map.remove("c")

        map["a"].shouldBeNull()
        map.size shouldBe 1
        map.names shouldContainExactly listOf("b")

        map["a"] = THIRD
        map["a"] shouldBe THIRD
        map.size shouldBe 2
    }

    test("forgets every name when cleared") {
        val map = NameMap<Int>()
        map["a"] = 1
        map.clear()

        map.size shouldBe 0
        map.entries.shouldBeEmpty()
        map["a"].shouldBeNull()
        map["b"] = 2
        map.names shouldContainExactly listOf("b")
    }

    test("agrees with a map of the same steps") {
        val step = Arb.pair(k = Arb.int(0 until NAME_RANGE), v = Arb.int(REMOVAL..NAME_RANGE))
        checkAll(Arb.list(step, 0..MOST_STEPS)) { steps ->
            val map = NameMap<Int>()
            val expected = HashMap<String, Int>()
            steps.forEach { (number, value) ->
                val name = nameOf(number)
                if (value == REMOVAL) map.remove(name)
                if (value == REMOVAL) expected.remove(name)
                if (value != REMOVAL) map[name] = value
                if (value != REMOVAL) expected[name] = value
            }

            map.size shouldBe expected.size
            for (number in 0 until NAME_RANGE) map[nameOf(number)] shouldBe expected[nameOf(number)]
        }
    }
})
