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
package com.davils.kaml.core.compose

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.yaml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val ITERATIONS: Int = 500
private const val MOST_PIECES: Int = 24
private const val FEW_NODES: Long = 3L
private const val FEW_DOCUMENTS: Int = 1

private val pieces: List<String> = listOf(
    "a", "1", " ", "\n", "- ", ": ", "[", "]", "{", "}", ",", "#c", "&a ", "&b ", "*a", "!t ", "!!str ", "!e!x ",
    "%YAML 1.2\n", "%YAML 1.1\n", "%TAG !e! tag:e,2026:\n", "%FOO\n", "---\n", "...\n", "\"", "'", "|\n", "? ",
)

private val documents: Arb<String> =
    Arb.list(Arb.element(pieces), 0..MOST_PIECES).map { chosen -> chosen.joinToString("") }

private fun Yaml.shouldComposeAsParsed(text: String) {
    val composed = YamlSyntaxTree.parse(text, this).compose()
    composed.diagnostics shouldBe parse(text).diagnostics
}

class ComposedTreeTest : FunSpec({
    context("a syntax tree composed into nodes") {
        test("reports a repeated property once, as a parse does") {
            Yaml.Default.shouldComposeAsParsed(text("&a &a x", "x", "%YAML 1.2", ""))
            Yaml.Default.shouldComposeAsParsed("!t !t x")
        }

        test("reports an exhausted budget once, as a parse does") {
            yaml { limits { maximumNodes = FEW_NODES } }.shouldComposeAsParsed("[a, b, c, d, e]")
            yaml { limits { maximumDocuments = FEW_DOCUMENTS } }.shouldComposeAsParsed(text("--- a", "--- b"))
        }

        test("reports the diagnostics of a parse for any document") {
            checkAll(ITERATIONS, documents) { text -> Yaml.Default.shouldComposeAsParsed(text) }
        }
    }
})
