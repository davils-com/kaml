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
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.fixture.problems
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val LINE_BREAK: String = "\n"
private const val MARK: String = "\uFEFF"

private val insideDocuments: Map<String, String> = mapOf(
    "a block mapping" to text("a: 1", MARK + "b: 2"),
    "a nested block mapping" to text("a:", "  x: 1", MARK + "  y: 2"),
    "a block sequence" to text("- a", MARK + "- b"),
    "a plain scalar" to text("a", MARK + "b"),
    "a line of a plain scalar" to "a" + MARK + "b",
    "a comment" to text("a: 1 # c" + MARK, "b: 2"),
    "a block scalar" to text("a: |", "  x" + MARK + "y"),
)

private val startsOfDocuments: Map<String, String> = mapOf(
    "the stream" to MARK + "a: 1",
    "the stream after a comment" to text("# c", MARK + "a: 1"),
    "a document after an end marker" to text("a", "...", MARK + "b"),
    "an explicit document after a bare one" to text("a", MARK + "--- b"),
    "an end marker" to text("a", MARK + "..."),
    "the end of the stream" to text("a", "...", MARK + "# c"),
)

class ByteOrderMarkTest : FunSpec({
    context("a byte order mark") {
        insideDocuments.forEach { (name, text) ->
            test("is an error inside $name") {
                Yaml.Default.problems(text) shouldContain YamlDiagnosticCode.UnexpectedContent
            }
        }

        test("is read the same by an incremental reparse as by a full parse") {
            val tree = YamlSyntaxTree.parse(text("---x", "", "'q", ": v", "##'q", "...", MARK + "# c", "|", ""))
            val edited = tree.edit(listOf(TextEdit(TextRange.at(1), "--- a" + LINE_BREAK)))
            edited.diagnostics shouldBe YamlSyntaxTree.parse(edited.text).diagnostics
        }

        startsOfDocuments.forEach { (name, text) ->
            test("is accepted at the start of $name") {
                Yaml.Default.parse(text).shouldBeInstanceOf<YamlParseResult.Parsed>()
            }
        }
    }
})
