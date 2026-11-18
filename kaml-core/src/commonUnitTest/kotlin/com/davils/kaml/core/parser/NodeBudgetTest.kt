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

import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.source.YamlSource
import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.internal.GreenElement
import com.davils.kaml.core.yaml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val BUDGET: Long = 10L
private const val ITEMS: Int = 100_000
private const val MOST_BUILT: Int = 16
private const val PAIR_NODES: Long = 4L

private val flatDocuments: Map<String, String> = mapOf(
    "a flow sequence" to "[\n" + "a,\n".repeat(ITEMS) + "a]",
    "a flow mapping" to "{" + "a: b, ".repeat(ITEMS) + "}",
    "a block sequence" to "- a\n".repeat(ITEMS),
    "a block mapping" to (0 until ITEMS).joinToString("") { index -> "k$index: v\n" },
    "a stream of documents" to "--- a\n".repeat(ITEMS),
)

private fun builtNodes(element: GreenElement): Int {
    val isNode = element.kind.isValueNode || element.kind == YamlSyntaxKind.FLOW_PAIR
    val own = 1.takeIf { isNode } ?: 0
    return own + element.children.sumOf(::builtNodes)
}

class NodeBudgetTest : FunSpec({
    flatDocuments.forEach { (name, text) ->
        context(name) {
            test("stops the parser at the node budget") {
                val parsed = SyntaxParser.parse(text, maximumNodes = BUDGET)
                parsed.diagnostics.map { it.code } shouldContain YamlDiagnosticCode.TooManyNodes
                builtNodes(parsed.root) shouldBeLessThan MOST_BUILT
                parsed.root.text shouldBe text
            }
        }
    }

    context("the node budget") {
        val tight = yaml { limits { maximumNodes = 3 } }

        test("holds a document with as many nodes as it allows") {
            tight.parse("[a, b]").shouldBeInstanceOf<YamlParseResult.Parsed>()
            tight.parse("a: b").shouldBeInstanceOf<YamlParseResult.Parsed>()
        }

        test("counts a single pair of a flow sequence as a mapping") {
            val pair = "[a: b]"
            tight.parse(pair).shouldBeInstanceOf<YamlParseResult.Failed>()
            val enough = yaml { limits { maximumNodes = PAIR_NODES } }
            enough.parse(pair).shouldBeInstanceOf<YamlParseResult.Parsed>()
        }

        test("is reported once for a document with more nodes") {
            val codes = tight.events(YamlSource.of("[a, b, c, d, e]")).diagnostics.map { it.code }
            codes shouldContainExactly listOf(YamlDiagnosticCode.TooManyNodes)
        }
    }
})
