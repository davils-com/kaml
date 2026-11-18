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

import com.davils.kaml.core.emit.YamlLineBreak
import com.davils.kaml.core.fixture.lines
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.edit.fixture.composed
import com.davils.kaml.core.syntax.edit.fixture.edited
import com.davils.kaml.core.syntax.edit.fixture.refusal
import com.davils.kaml.core.syntax.fixture.hostileTexts
import com.davils.kaml.core.yaml
import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.unsafeCodePointsIn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll

private const val PORT: Int = 8080
private const val NEW_PORT: Int = 9090
private const val SERVER: String = "server:"
private const val HOST: String = "  host: a"
private const val ITEMS: String = "items:"
private const val THIRD: Int = 3
private const val MISSING_INDEX: Int = 4
private const val CRLF_TEXT: String = "a: 1\r\nb: 2\r\n"
private const val ITEM_Y: String = "  - y"
private const val FLOW_MAPPING: String = "m: {a: 1}"
private const val NESTED_B: String = "  b: 2"
private const val NESTED_A: String = "  - a"
private const val LONGEST_KEY: Int = 1024
private const val CASES: Int = 100
private const val ENTRY_LINE: String = "a: 1\n"
private const val TWO_LINES: String = "x\ny"
private const val BLOCK_TWO_LINES: String = "a: |-"
private const val FLOW_ENTRY: String = "{a: 1}"

class YamlEditorTest : FunSpec({
    val root = YamlPath.ROOT
    val server = root.key("server")
    val items = root.key("items")

    context("set") {
        val text = lines(SERVER, "  port: $PORT # default", HOST)

        test("replaces a scalar and keeps its trailing comment") {
            edited(
                text,
            ) {
                set(
                    server.key("port"),
                    yamlScalar(NEW_PORT),
                )
            } shouldBe lines(SERVER, "  port: $NEW_PORT # default", HOST)
        }

        test("inserts a missing key after the last entry") {
            edited(
                text,
            ) {
                set(
                    server.key("name"),
                    yamlScalar("x"),
                )
            } shouldBe lines(SERVER, "  port: $PORT # default", HOST, "  name: x")
        }

        test("nests a mapping below its key") {
            val value = yamlMapping { "a" to yamlScalar(1) }

            edited(
                text,
            ) { set(server.key("port"), value) } shouldBe lines(SERVER, "  port: # default", "    a: 1", HOST)
        }

        test("keeps a comment between the indicator and the value") {
            edited(
                lines("key: # note", "  a: 1"),
            ) { set(root.key("key"), yamlScalar("v")) } shouldBe lines("key: # note", "  v")
        }

        test("fills an empty value") {
            edited(lines("a:", "b: 2")) { set(root.key("a"), yamlScalar(1)) } shouldBe lines("a: 1", "b: 2")
        }

        test("replaces a sequence item whatever its spacing") {
            edited(
                lines(ITEMS, "  -   x", ITEM_Y),
            ) { set(items.index(0), yamlScalar("z")) } shouldBe lines(ITEMS, "  - z", ITEM_Y)
        }

        test("writes a mapping item in compact form") {
            val value = yamlMapping {
                "a" to yamlScalar(1)
                "b" to yamlScalar(2)
            }

            edited(
                lines(ITEMS, "  - x", ITEM_Y),
            ) { set(items.index(1), value) } shouldBe lines(ITEMS, "  - x", "  - a: 1", "    b: 2")
        }

        test("writes a multi-line string as a block scalar that keeps its value") {
            val value = yamlScalar("first\nsecond")
            val expected = yamlMapping {
                "a" to yamlMapping { "b" to value }
                "c" to yamlScalar(2)
            }

            composed(lines("a:", "  b: 1", "c: 2")) { set(root.key("a").key("b"), value) } shouldBe expected
        }

        test("edits flow mappings in flow style") {
            edited(FLOW_MAPPING) { set(root.key("m").key("a"), yamlScalar(2)) } shouldBe "m: {a: 2}"
            edited(FLOW_MAPPING) { set(root.key("m").key("b"), yamlSequence { add("x") }) } shouldBe "m: {a: 1, b: [x]}"
        }

        test("writes empty and flow collections inline") {
            edited("a: 1") { set(root.key("a"), yamlSequence {}) } shouldBe "a: []"
            edited(
                FLOW_MAPPING,
            ) { set(root.key("m").key("b"), yamlMapping { "x" to yamlScalar(1) }) } shouldBe "m: {a: 1, b: {x: 1}}"
        }

        test("keeps the line breaks of the document") {
            edited(CRLF_TEXT) { set(root.key("c"), yamlScalar(THIRD)) } shouldBe CRLF_TEXT + "c: 3\r\n"
        }
    }

    context("append") {
        test("adds an item to a block sequence") {
            edited(lines("- a", "- b")) { append(root, yamlScalar("c")) } shouldBe lines("- a", "- b", "- c")
        }

        test("adds an item to a nested block sequence") {
            edited(
                lines("x:", NESTED_A, "y: 1"),
            ) { append(root.key("x"), yamlScalar("b")) } shouldBe lines("x:", NESTED_A, "  - b", "y: 1")
        }

        test("adds an item to a flow sequence") {
            edited("items: [a, b]") { append(items, yamlScalar("c")) } shouldBe "items: [a, b, c]"
        }

        test("adds the first item to an empty flow sequence") {
            edited("items: []") { append(items, yamlScalar("c")) } shouldBe "items: [c]"
        }

        test("adds the first entry to an empty flow mapping") {
            edited("m: {}") { set(root.key("m").key("a"), yamlScalar(1)) } shouldBe FLOW_MAPPING
        }
    }

    context("insert") {
        test("puts an item before the item at the index of a block sequence") {
            edited(lines("- a", "- b")) { insert(root.index(1), yamlScalar("x")) } shouldBe lines("- a", "- x", "- b")
        }

        test("keeps the indentation of a nested block sequence") {
            edited(
                lines("x:", NESTED_A, "y: 1"),
            ) { insert(root.key("x").index(0), yamlScalar("b")) } shouldBe lines("x:", "  - b", NESTED_A, "y: 1")
        }

        test("puts an item before the item at the index of a flow sequence") {
            edited("items: [a, b]") { insert(items.index(1), yamlScalar("x")) } shouldBe "items: [a, x, b]"
        }

        test("appends when the index is the size of the sequence") {
            edited("items: [a]") { insert(items.index(1), yamlScalar("x")) } shouldBe "items: [a, x]"
        }

        test("refuses an index beyond the end, a key and a mapping") {
            refusal("items: [a]") { insert(items.index(THIRD), yamlScalar("x")) } shouldBe
                YamlEditRefusal.MissingPath
            refusal(FLOW_MAPPING) { insert(root.key("m").key("a"), yamlScalar("x")) } shouldBe
                YamlEditRefusal.NotASequence
            refusal(FLOW_MAPPING) { insert(root.key("m").index(0), yamlScalar("x")) } shouldBe
                YamlEditRefusal.NotASequence
        }

        test("refuses an item that shares its line with its parent") {
            refusal("- - a") { insert(root.index(0).index(0), yamlScalar("x")) } shouldBe
                YamlEditRefusal.Unsupported
        }
    }

    context("remove") {
        test("removes a block entry with its line") {
            edited(lines("a: 1", "b: 2", "c: 3")) { remove(root.key("b")) } shouldBe lines("a: 1", "c: 3")
        }

        test("removes the last block entry") {
            edited(lines("a: 1", "b:", "  c: 2")) { remove(root.key("b")) } shouldBe lines("a: 1", "")
        }

        test("removes a block item") {
            edited(lines("- a", "- b")) { remove(root.index(0)) } shouldBe "- b"
        }

        test("removes flow items with their separators") {
            edited("[a, b, c]") { remove(root.index(1)) } shouldBe "[a, c]"
            edited("[a, b, c]") { remove(root.index(2)) } shouldBe "[a, b]"
            edited("[a]") { remove(root.index(0)) } shouldBe "[]"
        }

        test("removes a flow entry") {
            edited("{a: 1, b: 2}") { remove(root.key("a")) } shouldBe "{b: 2}"
        }
    }

    context("rename and comment") {
        test("renames a key and keeps its value") {
            edited(
                lines("a: 1 # note", "b: 2"),
            ) { rename(root.key("a"), "first key") } shouldBe lines("first key: 1 # note", "b: 2")
        }

        test("quotes a key that would otherwise change its meaning") {
            edited("a: 1") { rename(root.key("a"), "true") } shouldBe """"true": 1"""
        }

        test("writes comment lines above an entry") {
            val expected = lines("a: 1", "s:", "  # Note", "  #", NESTED_B)

            edited(
                lines("a: 1", "s:", NESTED_B),
            ) { comment(root.key("s").key("b"), listOf("Note", "")) } shouldBe expected
        }
    }

    context("refusals") {
        val text = lines("a: 1", "s: [x]")
        val deepMissing = root.key("x").key("y")
        val belowScalar = root.key("a").key("b")
        val belowIndex = root.key("a").index(0)

        test("name the missing document") {
            refusal(
                "a: 1",
                document = 2,
            ) { set(root.key("a"), yamlScalar(1)) } shouldBe YamlEditRefusal.MissingDocument
        }

        test("name a missing path") {
            refusal(text) { set(root, yamlScalar(1)) } shouldBe YamlEditRefusal.MissingPath
            refusal(text) { set(root.key("x").key("y"), yamlScalar(1)) } shouldBe YamlEditRefusal.MissingPath
            refusal(
                text,
            ) { set(deepMissing.key("z"), yamlScalar(1)) } shouldBe YamlEditRefusal.MissingPath
            refusal(
                text,
            ) { set(belowScalar.key("c"), yamlScalar(1)) } shouldBe YamlEditRefusal.MissingPath
            refusal(
                text,
            ) { set(belowIndex.key("c"), yamlScalar(1)) } shouldBe YamlEditRefusal.MissingPath
            refusal(
                text,
            ) { set(root.key("s").index(MISSING_INDEX), yamlScalar(1)) } shouldBe YamlEditRefusal.MissingPath
            refusal(text) { remove(root.key("x")) } shouldBe YamlEditRefusal.MissingPath
            refusal(text) { remove(root.key("s").index(MISSING_INDEX)) } shouldBe YamlEditRefusal.MissingPath
            refusal(text) { rename(root.key("x"), "y") } shouldBe YamlEditRefusal.MissingPath
            refusal(text) { comment(root.key("s").index(0), listOf("c")) } shouldBe YamlEditRefusal.MissingPath
        }

        test("name a parent of the wrong kind") {
            refusal(text) { set(root.key("s").key("b"), yamlScalar(1)) } shouldBe YamlEditRefusal.NotAMapping
            refusal(text) { remove(root.key("s").key("b")) } shouldBe YamlEditRefusal.NotAMapping
            refusal(text) { set(root.index(0), yamlScalar(1)) } shouldBe YamlEditRefusal.NotASequence
            refusal(text) { remove(root.index(0)) } shouldBe YamlEditRefusal.NotASequence
            refusal(text) { append(root, yamlScalar(1)) } shouldBe YamlEditRefusal.NotASequence
        }

        test("refuse edits they cannot write without changing the layout") {
            refusal(
                lines("- a: 1", NESTED_B),
            ) { remove(root.index(0).key("a")) } shouldBe YamlEditRefusal.Unsupported
            refusal(FLOW_ENTRY) { comment(root.key("a"), listOf("c")) } shouldBe YamlEditRefusal.Unsupported
            refusal("? a") { set(root.key("a"), yamlScalar(1)) } shouldBe YamlEditRefusal.Unsupported
        }

        test("carry the path they refused") {
            val editor = YamlEditor(YamlSyntaxTree.parse(text))
            val result = editor.remove(root.key("x"))

            result shouldBe YamlEditResult.Refused(YamlEditRefusal.MissingPath, root.key("x"))
        }
    }

    context("multi-line values") {
        test("move a comment behind the old value to the line of its key") {
            edited("a: 1 # note") { set(root.key("a"), yamlScalar(TWO_LINES)) } shouldBe
                lines("a: |- # note", "  x", "  y")
        }

        test("move a comment behind an old item to the line of its indicator") {
            val value = yamlMapping { "a" to yamlScalar(TWO_LINES) }

            edited(lines("- old # note", "- b")) { set(root.index(0), value) } shouldBe
                lines("- a: |- # note", "    x", "    y", "- b")
        }

        test("replace a block scalar without joining the line after it") {
            edited(lines("a: |", "  x", "  y", "b: 2")) { set(root.key("a"), yamlScalar(1)) } shouldBe
                lines("a: 1", "b: 2")
        }

        test("keep the trailing line breaks of a block scalar") {
            composed("a: 1\nb: 2\n") { set(root.key("a"), yamlScalar("x\n\n")) } shouldBe yamlMapping {
                "a" to yamlScalar("x\n\n")
                "b" to yamlScalar(2)
            }
        }

        test("keep an indentation indicator relative to their entry") {
            val value = yamlScalar("  x\ny")

            composed(lines(ITEMS, "  - a")) { set(items.index(0), value) } shouldBe
                yamlMapping { "items" to yamlSequence { add(value) } }
            composed(lines("a: # note", "  b")) { set(root.key("a"), value) } shouldBe
                yamlMapping { "a" to value }
        }

        test("refuse to move text behind the old value that is no comment") {
            refusal("a: 'x' y\n") { set(root.key("a"), yamlScalar(TWO_LINES)) } shouldBe YamlEditRefusal.Unsupported
        }
    }

    context("shared values") {
        test("are written once with an anchor and an alias") {
            val shared = yamlMapping { "a" to yamlScalar(1) }
            val value = yamlSequence(listOf(shared, shared))

            composed(ENTRY_LINE) { set(root.key("a"), value) } shouldBe yamlMapping { "a" to value }
        }
    }

    context("configurations") {
        test("write values without the document marker the emitter adds") {
            val marked = yaml { emitter { isDocumentStartExplicit = true } }
            val tree = YamlSyntaxTree.parse(ENTRY_LINE, marked)

            edited(ENTRY_LINE, tree) { set(root.key("a"), yamlScalar(2)) } shouldBe "a: 2\n"
            edited(ENTRY_LINE, tree) { set(root.key("a"), yamlMapping { "b" to yamlScalar(1) }) } shouldBe
                lines("a:", "  b: 1", "")
            edited(ENTRY_LINE, tree) { set(root.key("a"), yamlScalar(TWO_LINES)) } shouldBe
                lines(BLOCK_TWO_LINES, "  x", "  y", "")
        }

        test("write the line breaks of the document whatever the emitter uses") {
            val windows = yaml { emitter { lineBreak = YamlLineBreak.CrLf } }
            val tree = YamlSyntaxTree.parse(ENTRY_LINE, windows)

            edited(ENTRY_LINE, tree) { set(root.key("a"), yamlScalar(TWO_LINES)) } shouldNotContain "\r"
        }

        test("refuse flow values when the emitter does not preserve collection styles") {
            val plain = yaml { emitter { isStylePreserved = false } }
            val tree = YamlSyntaxTree.parse(FLOW_MAPPING, plain)
            val editor = YamlEditor(tree)

            editor.set(root.key("m").key("a"), yamlScalar(2)).shouldBeInstanceOf<YamlEditResult.Refused>()
            editor.set(root.key("m").key("b"), yamlScalar(2)).shouldBeInstanceOf<YamlEditResult.Refused>()
            val items = yamlSequence {
                add("x")
                add("y")
            }

            editor.set(root.key("m").key("a"), items).shouldBeInstanceOf<YamlEditResult.Refused>()
            editor.set(root.key("x"), yamlScalar(2)).shouldBeInstanceOf<YamlEditResult.Edited>()
        }

        test("refuse items of flow sequences when the emitter does not preserve collection styles") {
            val plain = yaml { emitter { isStylePreserved = false } }
            val editor = YamlEditor(YamlSyntaxTree.parse("[a]", plain))

            editor.append(root, yamlScalar("b")).shouldBeInstanceOf<YamlEditResult.Refused>()
            editor.insert(root.index(0), yamlScalar("b")).shouldBeInstanceOf<YamlEditResult.Refused>()
            editor.set(root.index(0), yamlScalar("b")).shouldBeInstanceOf<YamlEditResult.Refused>()
        }
    }

    context("keys") {
        test("are written plain up to the length of an implicit key") {
            val longest = "k".repeat(LONGEST_KEY)

            edited("a: 1") { rename(root.key("a"), longest) } shouldBe "$longest: 1"
        }

        test("are refused when they would be longer than an implicit key") {
            val longer = "k".repeat(LONGEST_KEY + 1)

            refusal("a: 1") { rename(root.key("a"), longer) } shouldBe YamlEditRefusal.Unsupported
            refusal("a: 1") { set(root.key(longer), yamlScalar(1)) } shouldBe YamlEditRefusal.Unsupported
        }

        test("are quoted when they hold a document marker or an indicator") {
            edited("a: 1") { rename(root.key("a"), "---") } shouldBe "\"---\": 1"
            edited("a: 1") { rename(root.key("a"), "a: b") } shouldBe "\"a: b\": 1"
            edited(FLOW_ENTRY) { rename(root.key("a"), "a,b") } shouldBe "{\"a,b\": 1}"
            edited(FLOW_ENTRY) { rename(root.key("a"), "?a") } shouldBe "{\"?a\": 1}"
            edited(FLOW_ENTRY) { set(root.key(":b"), yamlScalar("?c")) } shouldBe """{a: 1, ":b": "?c"}"""
        }
    }

    context("comments") {
        test("split a comment at its line breaks") {
            edited("a: 1") { comment(root.key("a"), listOf("first\nsecond")) } shouldBe
                lines("# first", "# second", "a: 1")
        }

        test("refuse a comment that could break the document or hide text") {
            val editor = YamlEditor(YamlSyntaxTree.parse("a: 1"))

            shouldThrow<IllegalArgumentException> { editor.comment(root.key("a"), listOf(HostileFragments.ESCAPE)) }
            shouldThrow<IllegalArgumentException> {
                editor.comment(root.key("a"), listOf(HostileFragments.RIGHT_TO_LEFT_OVERRIDE))
            }
            shouldThrow<IllegalArgumentException> {
                editor.comment(root.key("x"), listOf(HostileFragments.LINE_SEPARATOR))
            }
        }

        test("refuse a comment with an invisible character and keep its tabs") {
            val editor = YamlEditor(YamlSyntaxTree.parse("a: 1"))

            shouldThrow<IllegalArgumentException> {
                editor.comment(root.key("a"), listOf("soft" + HostileFragments.SOFT_HYPHEN + "hyphen"))
            }
            edited("a: 1") { comment(root.key("a"), listOf("tab\there")) } shouldBe lines("# tab\there", "a: 1")
        }

        test("write nothing for no lines") {
            edited("a: 1") { comment(root.key("a"), emptyList()) } shouldBe "a: 1"
        }
    }

    context("results") {
        val tree = YamlSyntaxTree.parse("a: 1")
        val edits = listOf(TextEdit(TextRange(start = 3, end = 4), "2"))

        test("compare by their content") {
            val edited = YamlEditResult.Edited(edits, tree)
            val refused = YamlEditResult.Refused(YamlEditRefusal.MissingPath, root.key("a"))

            edited shouldBe YamlEditResult.Edited(edits, tree)
            edited.hashCode() shouldBe YamlEditResult.Edited(edits, tree).hashCode()
            edited shouldNotBe YamlEditResult.Edited(emptyList(), tree)
            edited shouldNotBe YamlEditResult.Edited(edits, YamlSyntaxTree.parse("a: 1"))
            edited.equals(refused) shouldBe false
            refused shouldBe YamlEditResult.Refused(YamlEditRefusal.MissingPath, root.key("a"))
            refused.hashCode() shouldBe YamlEditResult.Refused(YamlEditRefusal.MissingPath, root.key("a")).hashCode()
            refused shouldNotBe YamlEditResult.Refused(YamlEditRefusal.Unsupported, root.key("a"))
            refused shouldNotBe YamlEditResult.Refused(YamlEditRefusal.MissingPath, root.key("b"))
            refused.equals(edited) shouldBe false
        }

        test("keep a copy of their edits") {
            val changing = edits.toMutableList()
            val edited = YamlEditResult.Edited(changing, tree)
            changing.clear()

            edited.edits shouldBe edits
        }

        test("describe themselves without the text they hold") {
            YamlEditResult.Edited(edits, tree).toString() shouldBe "Edited(1 edits, $tree)"
            YamlEditResult.Refused(YamlEditRefusal.MissingPath, root.key("a").index(0)).toString() shouldBe
                "Refused(MissingPath at /\"a\"/0)"
        }

        test("escape the keys of the path they refused") {
            checkAll(CASES, hostileTexts) { text ->
                val refused = YamlEditResult.Refused(YamlEditRefusal.MissingPath, root.key(text).index(1))

                unsafeCodePointsIn(refused.toString()).shouldBeEmpty()
                refused.toString() shouldContain "MissingPath"
            }
        }
    }

    context("paths") {
        test("describe themselves") {
            val path = root.key("a").index(1)

            path.toString() shouldBe "/\"a\"/1"
            root.toString() shouldBe "/"
            root.parent shouldBe null
            root.last shouldBe null
            root.key("a").parent shouldBe root
            path.last shouldBe YamlPath.Segment.Index(1)
            root.key("a").hashCode() shouldBe root.key("a").hashCode()
            root.key("a").equals("/a") shouldBe false
        }

        test("compare their segments") {
            val key = YamlPath.Segment.Key("a")
            val index = YamlPath.Segment.Index(1)

            key shouldBe YamlPath.Segment.Key("a")
            key.hashCode() shouldBe YamlPath.Segment.Key("a").hashCode()
            key shouldNotBe YamlPath.Segment.Key("b")
            key.equals(index) shouldBe false
            index shouldBe YamlPath.Segment.Index(1)
            index.hashCode() shouldBe 1
            index shouldNotBe YamlPath.Segment.Index(2)
            index.equals(key) shouldBe false
            root.key("1") shouldNotBe root.index(1)
        }

        test("escape their keys") {
            checkAll(CASES, hostileTexts) { text ->
                val path = root.key(text).index(0)

                unsafeCodePointsIn(path.toString()).shouldBeEmpty()
                unsafeCodePointsIn(YamlPath.Segment.Key(text).toString()).shouldBeEmpty()
                path.segments shouldBe listOf(YamlPath.Segment.Key(text), YamlPath.Segment.Index(0))
            }
        }
    }
})
