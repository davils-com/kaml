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
import io.kotest.matchers.shouldBe

class NameSetTest : FunSpec({
    test("holds every name once, in the order it first came") {
        val set = NameSet(listOf("b", "a", "b"))

        set.add("c") shouldBe true
        set.add("a") shouldBe false
        set.toList() shouldContainExactly listOf("b", "a", "c")
        set shouldBe setOf("a", "b", "c")
        ("a" in set) shouldBe true
        ("d" in set) shouldBe false
    }

    test("removes names and forgets them all when cleared") {
        val set = NameSet(listOf("a", "b"))
        set.remove("a")

        set.toList() shouldContainExactly listOf("b")
        set.size shouldBe 1

        set.clear()
        set.toList().shouldBeEmpty()
    }
})
