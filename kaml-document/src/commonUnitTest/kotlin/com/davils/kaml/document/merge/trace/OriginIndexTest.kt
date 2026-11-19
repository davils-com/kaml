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

class OriginIndexTest : FunSpec({
    context("origins") {
        test("come from the nearest listed location, also below a location that is not listed") {
            val index = OriginIndex(mapOf(JsonPointer.of("a", "b") to "x", JsonPointer.of("a", "b", "c", "d") to "y"))

            index.originOf(JsonPointer.ROOT).shouldBeNull()
            index.originOf(JsonPointer.of("a")).shouldBeNull()
            index.originOf(JsonPointer.of("a", "b", "c")) shouldBe "x"
            index.originOf(JsonPointer.of("a", "b", "c", "d", "e")) shouldBe "y"
            index.originOf(JsonPointer.of("q")).shouldBeNull()
        }
    }
})
