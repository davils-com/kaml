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

import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class IdentityOrdinalsTest : FunSpec({
    context("identity ordinals") {
        test("number nodes by identity, densely and in the order they come, on every target") {
            val ordinalOf = identityOrdinals()
            val first = yamlScalar("a")
            val equal = yamlScalar("a")
            val empty = yamlMapping(emptyList())

            ordinalOf(first) shouldBe 0
            ordinalOf(equal) shouldBe 1
            ordinalOf(first) shouldBe 0
            ordinalOf(empty) shouldBe 2
            ordinalOf(equal) shouldBe 1
        }

        test("belong to one table only") {
            val first = identityOrdinals()
            val second = identityOrdinals()
            val node = yamlScalar("a")

            first(yamlScalar("b")) shouldBe 0
            first(node) shouldBe 1
            second(node) shouldBe 0
        }
    }
})
