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

package com.davils.kaml.core.schema

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.fixture.problems
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.internal.ScalarValue
import com.davils.kaml.core.yaml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private val malformed: List<String> = listOf("1:x", "1:", "1::2", "1:99999999999", "1:60", "0b", "0x", "0_9")

private val wellFormed: Map<String, String> = mapOf(
    "190:20:30" to "685230",
    "-1:30" to "-90",
    "0b1_0" to "2",
    "0x_1F" to "31",
    "017" to "15",
    "0_" to "0",
    "1_000" to "1000",
)

class Yaml11IntegersTest : FunSpec({
    context("an integer of YAML 1.1") {
        test("that does not match its pattern has no value") {
            malformed.forEach { text -> Yaml11Forms.valueOf(YamlTag.INTEGER, text) shouldBe null }
        }

        test("that matches its pattern has its value in decimal") {
            wellFormed.forEach { (text, decimal) ->
                Yaml11Forms.valueOf(YamlTag.INTEGER, text) shouldBe ScalarValue.Integer(decimal)
            }
        }

        test("that is malformed fails the parse with a diagnostic instead of an exception") {
            malformed.forEach { value ->
                val result = Yaml.Default.parse(text("%YAML 1.1", "--- !!int '$value'"))
                result.shouldBeInstanceOf<YamlParseResult.Failed>()
                result.diagnostics.map { it.code } shouldContain YamlDiagnosticCode.InvalidTaggedValue
            }
        }

        test("that is malformed fails a parse with the schema of YAML 1.1") {
            val legacy = yaml { schema = YamlSchema.Yaml11 }
            legacy.problems("key: !!int 1:x\n") shouldContain YamlDiagnosticCode.InvalidTaggedValue
        }
    }
})
