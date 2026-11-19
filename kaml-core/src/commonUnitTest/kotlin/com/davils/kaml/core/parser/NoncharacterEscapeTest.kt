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
package com.davils.kaml.core.parser

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.event.YamlEvent
import com.davils.kaml.core.fixture.problems
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.source.YamlSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

private const val REPLACEMENT: String = "\uFFFD"

private val refusedEscapes: List<String> = listOf(
    "\\uFFFF",
    "\\uFFFE",
    "\\U0000FFFF",
    "\\U0000FFFE",
    "\\uD800",
    "\\U00110000",
)

class NoncharacterEscapeTest : FunSpec({
    context("an escape") {
        refusedEscapes.forEach { escape ->
            test("of $escape is an invalid escape") {
                Yaml.Default.problems("\"a$escape\"") shouldContain YamlDiagnosticCode.InvalidEscape
            }

            test("of $escape stands for U+FFFD in the events of an invalid result") {
                val result = Yaml.Default.events(YamlSource.of("\"a$escape\""))
                result.isValid shouldBe false
                val scalar = result.events.filterIsInstance<YamlEvent.Scalar>().single()
                scalar.value shouldBe "a$REPLACEMENT"
            }
        }

        test("of a character next to the refused ones is read") {
            val text = """"\uFFFD\uFFFC\U0001FFFD""""
            val result = Yaml.Default.events(YamlSource.of(text))
            result.isValid shouldBe true
        }
    }

    context("a scalar built in code") {
        test("with U+FFFE or U+FFFF is refused by the emitter, which could not write it readably") {
            listOf("a\uFFFE", "\uFFFFb").forEach { value ->
                shouldThrow<IllegalArgumentException> { Yaml.Default.emit(yamlScalar(value)) }
            }
        }
    }
})
