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
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private const val FEW: Int = 1 shl 13
private const val MANY: Int = 1 shl 15
private const val GROWTH: Int = 8
private const val MOST_KEYS: Int = 200
private const val KEY_RANGE: Int = 50
private val slack: Duration = 200.milliseconds

private fun timed(work: () -> Unit): Duration {
    val mark = TimeSource.Monotonic.markNow()
    work()
    return mark.elapsedNow()
}

private fun ascending(count: Int): OrderedIndex<Int> {
    val index = OrderedIndex<Int>()
    repeat(count) { key -> index.ordinalFor(key) }
    return index
}

class OrderedIndexTest : FunSpec({
    context("ordinals") {
        test("are dense, in the order the keys first came, and found again") {
            val index = OrderedIndex<String>()

            index.ordinalFor("b") shouldBe 0
            index.ordinalFor("a") shouldBe 1
            index.ordinalFor("b") shouldBe 0
            index.ordinalOf("a") shouldBe 1
            index.ordinalOf("c").shouldBeNull()
            index.size shouldBe 2
        }

        test("match a map of the same keys") {
            checkAll(Arb.list(Arb.int(0 until KEY_RANGE), 0..MOST_KEYS)) { keys ->
                val index = OrderedIndex<Int>()
                val expected = LinkedHashMap<Int, Int>()
                keys.forEach { key -> index.ordinalFor(key) shouldBe expected.getOrPut(key) { expected.size } }

                for (key in 0 until KEY_RANGE) index.ordinalOf(key) shouldBe expected[key]
            }
        }
    }

    context("growth") {
        test("stays balanced for keys that come in order") {
            val fewTime = timed { ascending(FEW) }
            val manyTime = timed { ascending(MANY) }

            (manyTime < fewTime * GROWTH + slack) shouldBe true
            ascending(MANY).ordinalOf(MANY - 1) shouldBe MANY - 1
        }
    }
})
