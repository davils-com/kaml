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

package com.davils.kaml.core.node

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.text
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val INTEGER_ONE: String = "1: int"
private const val TAGGED_ROLE: String = "!x role: guest"

private val sameTexts: List<String> = listOf(
    text(INTEGER_ONE, "\"1\": str"),
    text("\"1\": str", INTEGER_ONE),
    text(TAGGED_ROLE, "role: str"),
    text("role: str", TAGGED_ROLE),
    text("true: bool", "'true': str"),
    text("~: null", "\"~\": str"),
)

class StringKeysTest : FunSpec({
    context("a string key") {
        sameTexts.forEach { document ->
            test("finds the key tagged str among keys of the same text in ${document.lines().first()}") {
                val mapping = Yaml.Default.root(document).shouldBeInstanceOf<YamlNode.Mapping>()
                val key = (mapping.keys.first { node -> node.tag == yamlScalar("").tag } as YamlNode.Scalar).value
                mapping[key].string() shouldBe "str"
                mapping[key] shouldBeSameInstanceAs mapping[yamlScalar(key)]
            }
        }

        test("finds no key of another tag") {
            Yaml.Default.root(INTEGER_ONE)["1"] shouldBe null
            Yaml.Default.root(TAGGED_ROLE)["role"] shouldBe null
        }
    }
})
