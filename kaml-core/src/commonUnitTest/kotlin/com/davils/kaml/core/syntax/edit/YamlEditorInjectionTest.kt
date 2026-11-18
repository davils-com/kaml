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

import com.davils.kaml.core.fixture.lines
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kaml.core.syntax.edit.fixture.composed
import com.davils.kaml.core.syntax.edit.fixture.isRepresentable
import com.davils.kaml.core.syntax.edit.fixture.unwritableTexts
import com.davils.kaml.core.syntax.edit.fixture.writableTexts
import com.davils.kaml.core.syntax.fixture.hostileTexts
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.checkAll

private const val CASES: Int = 100
private const val K0: String = "k0"
private const val K1: String = "k1"
private const val K2: String = "k2"
private const val ITEMS: String = "items"
private const val ONE: String = "one"
private const val OLD: String = "old"
private const val THREE: String = "three"
private const val FLOW: String = "m"

private const val FIRST_ENTRY: String = "k0: 1"
private const val LAST_ENTRY: String = "k2: 2"
private const val END: String = ""

private val blockMapping: String = lines(FIRST_ENTRY, "k1: old # note", LAST_ENTRY, END)
private val flowMapping: String = lines("m: {k0: 1, k1: old}", LAST_ENTRY, END)
private val blockSequence: String = lines("items:", "  - one", "  - old # note", "  - three", END)
private val flowSequence: String = lines("items: [one, old, three]", END)
private val spacedValue: String = lines("k0: # note", "  old", LAST_ENTRY, END)
private val windowsMapping: String = listOf(FIRST_ENTRY, "k1: old", END).joinToString("\r\n")
private val nestedEntry: String = lines("s:", "  k1: 1", LAST_ENTRY, END)

private fun entries(vararg entries: Pair<String, YamlNode>): YamlNode = yamlMapping {
    entries.forEach { (key, value) -> yamlScalar(key) to value }
}

private fun items(vararg items: YamlNode): YamlNode = yamlSequence { items.forEach(::add) }

private fun scalar(text: String): YamlNode = yamlScalar(text)

private fun isCommentable(text: String): Boolean = runCatching { EditInput.commentLines(listOf(text)) }.isSuccess

private fun mappingWith(key: String, value: YamlNode): YamlNode =
    entries(K0 to yamlScalar(1), key to value, K2 to yamlScalar(2))

private fun flowMappingWith(key: String, value: YamlNode): YamlNode =
    entries(FLOW to entries(K0 to yamlScalar(1), key to value), K2 to yamlScalar(2))

private fun listWith(vararg items: YamlNode): YamlNode = entries(ITEMS to items(*items))

class YamlEditorInjectionTest : FunSpec({
    val root = YamlPath.ROOT
    val k1 = root.key(K1)
    val listed = root.key(ITEMS)
    val inFlow = root.key(FLOW)

    context("a scalar value set through the editor") {
        test("parses back to exactly the string in a block mapping") {
            checkAll(CASES, writableTexts) { text ->
                composed(blockMapping) { set(k1, yamlScalar(text)) } shouldBe mappingWith(K1, scalar(text))
            }
        }

        test("parses back to exactly the string below a comment") {
            checkAll(CASES, writableTexts) { text ->
                composed(spacedValue) { set(root.key(K0), yamlScalar(text)) } shouldBe
                    entries(K0 to scalar(text), K2 to yamlScalar(2))
            }
        }

        test("parses back to exactly the string in a document with carriage returns") {
            checkAll(CASES, writableTexts) { text ->
                composed(windowsMapping) { set(k1, yamlScalar(text)) } shouldBe
                    entries(K0 to yamlScalar(1), K1 to scalar(text))
            }
        }

        test("parses back to exactly the string as a new entry") {
            checkAll(CASES, writableTexts) { text ->
                val expected = entries(
                    K0 to yamlScalar(1),
                    K1 to scalar(OLD),
                    K2 to yamlScalar(2),
                    "k3" to scalar(text),
                )

                composed(blockMapping) { set(root.key("k3"), yamlScalar(text)) } shouldBe expected
            }
        }

        test("parses back to exactly the string in a flow mapping") {
            checkAll(CASES, writableTexts) { text ->
                composed(flowMapping) { set(inFlow.key(K1), yamlScalar(text)) } shouldBe
                    flowMappingWith(K1, scalar(text))
            }
        }

        test("parses back to exactly the string as an item of a block sequence") {
            checkAll(CASES, writableTexts) { text ->
                composed(blockSequence) { set(listed.index(1), yamlScalar(text)) } shouldBe
                    listWith(scalar(ONE), scalar(text), scalar(THREE))
                composed(blockSequence) { insert(listed.index(1), yamlScalar(text)) } shouldBe
                    listWith(scalar(ONE), scalar(text), scalar(OLD), scalar(THREE))
                composed(blockSequence) { append(listed, yamlScalar(text)) } shouldBe
                    listWith(scalar(ONE), scalar(OLD), scalar(THREE), scalar(text))
            }
        }

        test("parses back to exactly the string as an item of a flow sequence") {
            checkAll(CASES, writableTexts) { text ->
                composed(flowSequence) { set(listed.index(1), yamlScalar(text)) } shouldBe
                    listWith(scalar(ONE), scalar(text), scalar(THREE))
                composed(flowSequence) { insert(listed.index(1), yamlScalar(text)) } shouldBe
                    listWith(scalar(ONE), scalar(text), scalar(OLD), scalar(THREE))
                composed(flowSequence) { append(listed, yamlScalar(text)) } shouldBe
                    listWith(scalar(ONE), scalar(OLD), scalar(THREE), scalar(text))
            }
        }
    }

    context("a collection set through the editor") {
        test("parses back to exactly its keys and values in block and flow style") {
            checkAll(CASES, writableTexts, writableTexts) { key, text ->
                val value = entries(key to items(scalar(text), scalar(OLD)))

                composed(blockMapping) { set(k1, value) } shouldBe mappingWith(K1, value)
                composed(blockSequence) { set(listed.index(1), value) } shouldBe
                    listWith(scalar(ONE), value, scalar(THREE))
                composed(flowMapping) { set(inFlow.key(K1), value) } shouldBe flowMappingWith(K1, value)
            }
        }
    }

    context("a key set through the editor") {
        test("parses back to exactly the string when an entry is added") {
            checkAll(CASES, writableTexts) { text ->
                val block = entries(
                    K0 to yamlScalar(1),
                    K1 to scalar(OLD),
                    K2 to yamlScalar(2),
                    text to scalar(OLD),
                )
                val flow = entries(
                    FLOW to entries(K0 to yamlScalar(1), K1 to scalar(OLD), text to scalar(OLD)),
                    K2 to yamlScalar(2),
                )

                composed(blockMapping) { set(root.key(text), yamlScalar(OLD)) } shouldBe block
                composed(flowMapping) { set(inFlow.key(text), yamlScalar(OLD)) } shouldBe flow
            }
        }

        test("parses back to exactly the string when an entry is renamed") {
            checkAll(CASES, writableTexts) { text ->
                composed(blockMapping) { rename(k1, text) } shouldBe mappingWith(text, scalar(OLD))
                composed(flowMapping) { rename(inFlow.key(K1), text) } shouldBe flowMappingWith(text, scalar(OLD))
            }
        }
    }

    context("text that YAML cannot represent") {
        test("is refused as a value, an item and a key") {
            checkAll(CASES, unwritableTexts) { text ->
                val editor = YamlEditor(YamlSyntaxTree.parse(blockSequence))
                val first = listed.index(0)

                shouldThrow<IllegalArgumentException> { editor.set(first, yamlScalar(text)) }
                shouldThrow<IllegalArgumentException> { editor.append(listed, items(scalar(text))) }
                shouldThrow<IllegalArgumentException> { editor.insert(first, entries(text to scalar(OLD))) }
                shouldThrow<IllegalArgumentException> { editor.set(root.key(text), yamlScalar(OLD)) }
                shouldThrow<IllegalArgumentException> { editor.rename(listed, text) }
            }
        }

        test("is told apart from text it can represent") {
            isRepresentable("a😀b") shouldBe true
            isRepresentable("\uD800") shouldBe false
            isRepresentable("\uDC00") shouldBe false
            isRepresentable("\uFFFE") shouldBe false
        }
    }

    context("a comment written through the editor") {
        val path = root.key("s").key(K1)

        test("keeps the document and reads back line by line") {
            checkAll(CASES, hostileTexts.filter(::isCommentable)) { text ->
                val editor = YamlEditor(YamlSyntaxTree.parse(nestedEntry))
                val edited = editor.comment(path, listOf(text)).shouldBeInstanceOf<YamlEditResult.Edited>()
                val document = edited.tree.documents.single()
                val mapping = document.root.shouldBeInstanceOf<YamlValueSyntax.Mapping>()
                val nested = mapping.entry("s")?.value.shouldBeInstanceOf<YamlValueSyntax.Mapping>()

                nested.entry(K1)?.leadingComments shouldBe EditInput.commentLines(listOf(text))
                composed(nestedEntry) { comment(path, listOf(text)) } shouldBe
                    entries("s" to entries(K1 to yamlScalar(1)), K2 to yamlScalar(2))
            }
        }

        test("is refused when it could break the document or hide text") {
            checkAll(CASES, hostileTexts.filterNot(::isCommentable)) { text ->
                val editor = YamlEditor(YamlSyntaxTree.parse(nestedEntry))

                shouldThrow<IllegalArgumentException> { editor.comment(path, listOf("fine", text)) }
            }
        }
    }
})
