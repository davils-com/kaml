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

package com.davils.kaml.document.internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val FIRST: Int = 1
private const val SECOND: Int = 2
private const val THIRD: Int = 3

class OrderedGroupsTest : FunSpec({
    context("groups") {
        test("keep their keys in the order they first came and their values in the order they were added") {
            val groups = OrderedGroups<String, Int>()
            groups.add("b", FIRST)
            groups.add("a", SECOND)
            groups.add("b", THIRD)

            groups.keys shouldBe listOf("b", "a")
            groups.values shouldBe listOf(listOf(FIRST, THIRD), listOf(SECOND))
            groups["b"] shouldBe listOf(FIRST, THIRD)
            groups["c"].shouldBeNull()
            ("a" in groups) shouldBe true
            ("c" in groups) shouldBe false
            groups.firstOf("b") shouldBe FIRST
            groups.firstOf("c").shouldBeNull()
            groups.firstOf(null).shouldBeNull()
            groups.all { _, values -> values.isNotEmpty() } shouldBe true
            groups.size shouldBe 2
        }
    }
})
