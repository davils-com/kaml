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

package com.davils.kaml.core.syntax.edit

import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.syntax.YamlSyntaxTree
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.pair
import io.kotest.property.checkAll

private const val CASES: Int = 3_000
private const val MOST_INDENT: Int = 4
private const val MOST_LINES: Int = 8
private const val SEVEN: Int = 7

private val editLines: List<String> = listOf(
    "a: 1", "b:", "c: 2", "- x", "- y: 1", "? a", ": v", "|+", ">-", "text", "# c", "", "---", "...", "\"q",
    "'s", "[a,", "b]", "{a: v,", "z: w}", "a: [1, 2]", "b: {c: 1}", "a: |+", "-", "&a b: 1", "*a : 2", "a: *a",
    "b: [c: 1]", "- - z", "a: 'x", "  y'", "b: \"x", "\"", "a: x # t", "\t", "b: !t", "a:", "  - c: 1",
)

private val indents: Arb<Int> = Arb.int(0..MOST_INDENT)

private val indentedLines: Arb<String> = Arb.pair(indents, Arb.element(editLines)).map { (indent, line) ->
    " ".repeat(indent) + line
}

private val documents: Arb<String> = Arb.list(indentedLines, 1..MOST_LINES).map { lines ->
    lines.joinToString(separator = "\n", postfix = "\n")
}

private val root: YamlPath = YamlPath.ROOT
private val keyB: YamlPath = root.key("b")

private val paths: List<YamlPath> = listOf(
    root.key("a"),
    keyB,
    root.key("new"),
    root.index(0),
    root.index(1),
    keyB.key("c"),
    root.key("a").index(0),
    keyB.index(0).key("c"),
    root.key("a").index(2),
    keyB.key("d"),
)

private val values: List<YamlNode> = listOf(
    yamlScalar("v"),
    yamlScalar("line\nbreak"),
    yamlScalar(""),
    yamlScalar(SEVEN),
    yamlSequence { add("x") },
    yamlMapping { "k" to "v" },
    yamlSequence(items = listOf(yamlScalar("f")), style = YamlCollectionStyle.Flow),
)

private val operations: List<YamlEditor.(YamlPath, YamlNode) -> YamlEditResult> = listOf(
    { path, value -> set(path, value) },
    { path, value -> append(path, value) },
    { path, value -> insert(path, value) },
    { path, _ -> remove(path) },
    { path, _ -> rename(path, "r: x") },
    { path, _ -> comment(path, listOf("note")) },
)

private fun outcomeOf(result: YamlParseResult): Pair<Boolean, List<YamlNode>> {
    val stream = when (result) {
        is YamlParseResult.Parsed -> result.stream
        is YamlParseResult.Failed -> result.partial
    }
    return (result is YamlParseResult.Parsed) to stream.documents.map { it.root }
}

class YamlEditorPropertyTest : FunSpec({
    context("every operation of the editor") {
        test("gives a tree that composes as a full parse of its text does") {
            checkAll(
                iterations = CASES,
                genA = documents,
                genB = Arb.element(paths),
                genC = Arb.element(values),
                genD = Arb.element(operations),
            ) { text, path, value, operation ->
                val result = YamlEditor(YamlSyntaxTree.parse(text)).operation(path, value)
                val edited = (result as? YamlEditResult.Edited)?.tree
                val full = edited?.let { YamlSyntaxTree.parse(it.text) }

                edited?.green shouldBe full?.green
                edited?.let { outcomeOf(it.compose()) } shouldBe full?.let { outcomeOf(it.compose()) }
            }
        }
    }
})
