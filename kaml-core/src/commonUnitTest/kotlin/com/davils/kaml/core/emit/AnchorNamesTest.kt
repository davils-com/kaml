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

package com.davils.kaml.core.emit

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AnchorNamesTest : FunSpec({
    context("an anchor name") {
        test("is valid when it holds only ASCII letters, digits, hyphens, underscores and dots") {
            listOf("a", "Z", "09", "a-b_c.d").forEach { name -> AnchorNames.isValid(name) shouldBe true }
        }

        test("is invalid when it is empty or holds any other character") {
            listOf("", "a b", "a:b", "a,b", "ä", "a\u202Eb", "a\nb", "*a").forEach { name ->
                AnchorNames.isValid(name) shouldBe false
            }
        }
    }

    context("the names of a document") {
        test("keep a valid name once and generate one for a taken, a missing or an invalid name") {
            val names = AnchorNames(reserved = setOf("id002"))

            names.preferred("base") shouldBe "base"
            names.preferred("base") shouldBe "id001"
            names.preferred(null) shouldBe "id003"
            names.preferred("a b") shouldBe "id004"
        }

        test("forget the names they used when they are cleared") {
            val names = AnchorNames()
            names.preferred("base")
            names.clear()

            names.preferred("base") shouldBe "base"
        }
    }
})
