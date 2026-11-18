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
import com.davils.kaml.core.fixture.problems
import com.davils.kaml.core.fixture.text
import com.davils.kore.testing.HostileFragments
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val ITERATIONS: Int = 1_000
private const val MOST_PIECES: Int = 48

private val pieces: List<String> = listOf(
    "a", "1", " ", "  ", "\t", "\n", "\r\n", "\r", "-", "- ", "?", "? ", ":", ": ", ",", "[", "]", "{", "}", "#",
    " #", "&a", "*a", "!", "!!str", "!<x>", "!e!", "|", ">", "|-", ">+2", "'", "\"", "\\", "\\u",
    "\\u1", "\\UD", "\\x4", "\\uD83D", "\\uDE00", "%YAML 1.2",
    "%TAG", "---", "...", "<<", "\uFEFF", "\uFFFF", "\uFFFE", "\u0000", "\u0085",
) +
    HostileFragments.all

private val documents: Arb<String> =
    Arb.list(Arb.element(pieces), 0..MOST_PIECES).map { chosen -> chosen.joinToString("") }

private val unusual: Map<String, YamlDiagnosticCode?> = mapOf(
    "a: 1\n\uFEFFb: 2" to null,
    "{a:\n--- b}" to YamlDiagnosticCode.UnterminatedFlowCollection,
    "[!t \"x\"]" to null,
    "[&a *b]" to YamlDiagnosticCode.AliasWithProperties,
    "[!t\n---\n]" to YamlDiagnosticCode.UnterminatedFlowCollection,
    "!e%C3%A x" to YamlDiagnosticCode.InvalidTag,
    text("%TAG abc tag:x,2026:", "---", "a") to YamlDiagnosticCode.InvalidDirective,
)

class RobustnessTest : FunSpec({
    context("any input") {
        test("is read to its end and kept in the syntax tree") {
            checkAll(ITERATIONS, documents) { text ->
                Yaml.Default.parse(text)
                SyntaxParser.parse(text).root.text shouldBe text
            }
        }
    }

    context("unusual layouts") {
        unusual.forEach { (source, code) ->
            test("are read to the end of ${source.replace(oldValue = "\n", newValue = "\\n")}") {
                SyntaxParser.parse(source).root.text shouldBe source
                if (code != null) Yaml.Default.problems(source) shouldContain code
            }
        }
    }

    context("a byte order mark") {
        test("does not stop a directive from ending") {
            Yaml.Default.problems("%YAML\uFEFF 1.2\n---\na") shouldContain YamlDiagnosticCode.ReservedDirective
            Yaml.Default.problems("%FOO a\uFEFFb\n---\na") shouldContain YamlDiagnosticCode.ReservedDirective
        }
    }
})
