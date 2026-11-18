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

package com.davils.kaml.document.path

import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.document.fixture.nodeOf
import com.davils.kaml.document.pointer.JsonPointer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private val location = JsonPointer.of("a")
private val one = yamlScalar(1)

class JsonPathMatchTest : FunSpec({
    context("matches") {
        test("are equal by location and node") {
            val match = JsonPathMatch(location, one)

            match shouldBe JsonPathMatch(location, yamlScalar(1))
            match.hashCode() shouldBe JsonPathMatch(location, one).hashCode()
            match shouldNotBe JsonPathMatch(JsonPointer.of("b"), one)
            match shouldNotBe JsonPathMatch(location, yamlScalar(2))
            match.equals(one) shouldBe false
            match.toString() shouldBe "JsonPathMatch(/a, Scalar)"
        }

        test("of the root query lie at the root") {
            val matches = JsonPath.parse("$").query(nodeOf("{a: 1}"))
            val root = matches.single()

            root.location shouldBe JsonPointer.ROOT
            JsonPath.parse("$") shouldNotBe JsonPath.parse("$.a")
        }
    }
})
