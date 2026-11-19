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

package com.davils.kaml.document.merge.trace

import com.davils.kaml.document.pointer.JsonPointer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class OriginMapTest : FunSpec({
    context("origins") {
        val a = JsonPointer.of("a")
        val b = JsonPointer.of("a", "b")
        val origins = OriginMap(listOf(JsonPointer.ROOT to "x", a to "x", b to "y", a to "z"))

        test("keep the first position and the last origin of a pointer given twice") {
            origins.size shouldBe THREE
            origins.keys.toList() shouldBe listOf(JsonPointer.ROOT, a, b)
            origins[a] shouldBe "z"
            origins[JsonPointer.of("q")].shouldBeNull()
            origins.containsKey(b) shouldBe true
            origins.containsKey(JsonPointer.of("a", "c")) shouldBe false
        }

        test("compare as maps and entries do") {
            val entry = origins.entries.first()

            origins shouldBe mapOf(JsonPointer.ROOT to "x", a to "z", b to "y")
            origins shouldNotBe mapOf(JsonPointer.ROOT to "x", a to "x", b to "y")
            entry shouldBe mapOf(JsonPointer.ROOT to "x").entries.first()
            entry shouldNotBe mapOf(JsonPointer.ROOT to "y").entries.first()
            entry shouldNotBe mapOf(a to "x").entries.first()
            entry.equals("x") shouldBe false
            entry.hashCode() shouldBe (JsonPointer.ROOT.hashCode() xor "x".hashCode())
            entry.toString() shouldBe "=x"
        }
    }
})

private const val THREE: Int = 3
