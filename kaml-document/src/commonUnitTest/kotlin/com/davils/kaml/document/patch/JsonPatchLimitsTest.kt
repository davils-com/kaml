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

package com.davils.kaml.document.patch

import com.davils.kaml.core.YamlDuplicateKeys
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.limit.YamlLimitsBuilder
import com.davils.kaml.core.limit.yamlLimits
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlNull
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.yaml
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.fixture.lines
import com.davils.kaml.document.fixture.nodeOf
import com.davils.kaml.document.fixture.treeOf
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kaml.document.pointer.toJsonPointer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.ArbitraryBuilderContext
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val TWO: Int = 2
private const val THREE: Int = 3
private const val COPIES: Int = 3
private const val ENOUGH_NODES: Long = 25L
private const val MANY_COPIES: Int = 64
private const val MOST_OPERATIONS: Int = 8
private const val MOST_PIECES: Int = 6
private const val ONE_MEMBER: String = "{a: 1}"
private const val TWO_MEMBERS: String = "{a: 1, b: 2}"
private const val SMALL_TREE: String = "a: 1"
private const val EMPTY: String = "{}"
private const val ALIASED: String = "a: &x {k: 1}"
private const val DEEP_MOVE: String = "{a: [[1]], b: []}"
private const val TWINS: String = "{true: 1, 'true': 2}"

private val operationNames = listOf("add", "remove", "replace", "move", "copy", "test", "drop", "")
private val pointerPieces = listOf("/", "a", "b", "0", "1", "-", "~0", "~1", "~", "~2", "", "01")
private val pointers: Arb<String> = Arb.list(Arb.element(pointerPieces), 0..MOST_PIECES).map { it.joinToString("") }
private val values: Arb<YamlNode> = Arb.element(
    listOf(yamlScalar(1), yamlScalar("x"), nodeOf("{a: [1]}"), nodeOf("[1, 2]"), yamlNull(), nodeOf("{b: {c: 1}}")),
)

private val operations: Arb<YamlNode> = arbitrary {
    val required = listOf(member("op", yamlScalar(Arb.element(operationNames).bind())), member("path", pointer()))
    val optional = listOf(member("from", pointer()), member("value", values.bind()))
    val isFull = Arb.boolean().bind()
    yamlMapping(required + optional.takeIf { isFull }.orEmpty())
}

private val patches: Arb<YamlNode> = Arb.list(operations, 0..MOST_OPERATIONS).map { yamlSequence(it) }

private suspend fun ArbitraryBuilderContext.pointer(): YamlNode = yamlScalar(pointers.bind())

private fun member(name: String, value: YamlNode): YamlEntry = YamlEntry(key = yamlScalar(name), value = value)

private fun limits(configure: YamlLimitsBuilder.() -> Unit): YamlLimits = yamlLimits(YamlLimits.Untrusted, configure)

private fun removals(count: Int): List<JsonPatchOperation> = List(
    count,
) { JsonPatchOperation.Remove("/a".toJsonPointer()) }

private fun copies(count: Int): List<JsonPatchOperation> =
    List(count) { index -> JsonPatchOperation.Copy(from = JsonPointer.ROOT, path = JsonPointer.of("c$index")) }

private fun nested(depth: Int): YamlNode =
    (1..depth).fold<Int, YamlNode>(yamlScalar("x")) { node, _ -> yamlSequence(listOf(node)) }

private infix fun JsonPatch.on(text: String): JsonPatchResult = apply(nodeOf(text))

private infix fun JsonPatch.onTree(text: String): JsonPatchEdit = apply(treeOf(text))

private infix fun JsonMergePatch.onTree(text: String): JsonPatchEdit = apply(treeOf(text))

private fun diff(source: String, target: String, limits: YamlLimits): JsonPatch =
    JsonPatch.diff(source = nodeOf(source), target = nodeOf(target), limits = limits)

class JsonPatchLimitsTest : FunSpec({
    context("operations") {
        test("a patch holds at most as many operations as its limits allow") {
            val two = limits { maximumPatchOperations = TWO }
            val accepted = JsonPatch.read(nodeOf("[{op: remove, path: /a}, {op: remove, path: /b}]"), two)
            val refused = JsonPatch.read(
                nodeOf("[{op: remove, path: /a}, {op: remove, path: /b}, {op: remove, path: /c}]"),
                two,
            )

            JsonPatch(removals(TWO), two).operations.size shouldBe TWO
            shouldThrow<IllegalArgumentException> { JsonPatch(removals(THREE), two) }
            accepted.shouldBeInstanceOf<JsonPatchReading.Read>()
            refused shouldBe JsonPatchReading.Invalid(-1, "a patch must not have more operations than its limits allow")
        }

        test("ignores members an operation does not define, as RFC 6902 requires") {
            val reading = JsonPatch.read(nodeOf("[{op: test, path: /a, value: 1, from: /b, spurious: 1}]"))
            val patch = reading.shouldBeInstanceOf<JsonPatchReading.Read>().patch

            patch on ONE_MEMBER shouldBe JsonPatchResult.Applied(nodeOf(ONE_MEMBER))
        }

        test("cannot be changed through the list they were made from") {
            val list = removals(1).toMutableList()
            val patch = JsonPatch(list)
            list.add(JsonPatchOperation.Remove(JsonPointer.ROOT))

            patch.operations.size shouldBe 1
        }
    }

    context("growth") {
        test("copies count every node of their value against the node budget") {
            val enough = JsonPatch(copies(COPIES), limits { maximumNodes = ENOUGH_NODES })
            val short = JsonPatch(copies(COPIES), limits { maximumNodes = ENOUGH_NODES - 1 })
            val many = JsonPatch(copies(MANY_COPIES)) on ONE_MEMBER

            (enough on ONE_MEMBER).shouldBeInstanceOf<JsonPatchResult.Applied>()
            short on ONE_MEMBER shouldBe JsonPatchResult.Failed(COPIES - 1, JsonPatchRefusal.LimitExceeded)
            many.shouldBeInstanceOf<JsonPatchResult.Failed>().reason shouldBe JsonPatchRefusal.LimitExceeded
            short onTree SMALL_TREE shouldBe JsonPatchEdit.Failed(COPIES - 1, JsonPatchRefusal.LimitExceeded)
        }

        test("a result deeper than the depth limit is refused") {
            val shallow = limits { maximumDepth = THREE }
            val add = { value: YamlNode ->
                JsonPatch(
                    listOf(JsonPatchOperation.Add("/a".toJsonPointer(), value)),
                    shallow,
                )
            }
            val move = JsonPatchOperation.Move(from = "/a".toJsonPointer(), path = "/b/0/0/0/0".toJsonPointer())

            (add(nested(TWO)) on EMPTY).shouldBeInstanceOf<JsonPatchResult.Applied>()
            add(nested(THREE)) on EMPTY shouldBe JsonPatchResult.Failed(0, JsonPatchRefusal.LimitExceeded)
            JsonPatch(listOf(move), shallow) on "{a: 1, b: [[[[1]]]]}" shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.LimitExceeded)
        }

        test("a move counts the depth of its value, not only of its path") {
            val shallow = limits { maximumDepth = THREE }
            val move = JsonPatch(
                listOf(JsonPatchOperation.Move(from = "/a".toJsonPointer(), path = "/b/0".toJsonPointer())),
                shallow,
            )

            (move on "{a: [1], b: []}").shouldBeInstanceOf<JsonPatchResult.Applied>()
            move on DEEP_MOVE shouldBe JsonPatchResult.Failed(0, JsonPatchRefusal.LimitExceeded)
            move onTree DEEP_MOVE shouldBe JsonPatchEdit.Failed(0, JsonPatchRefusal.LimitExceeded)
        }
    }

    context("test") {
        test("compares numbers exactly") {
            val big = "{a: 9007199254740993, b: 1, c: '1'}"
            val test = { path: String, value: String ->
                JsonPatch(listOf(JsonPatchOperation.Test(path.toJsonPointer(), nodeOf(value)))) on big
            }

            test("/a", "9007199254740992") shouldBe JsonPatchResult.Failed(0, JsonPatchRefusal.TestFailed)
            test("/a", "9007199254740993").shouldBeInstanceOf<JsonPatchResult.Applied>()
            test("/b", "1.0").shouldBeInstanceOf<JsonPatchResult.Applied>()
            test("/c", "1") shouldBe JsonPatchResult.Failed(0, JsonPatchRefusal.TestFailed)
        }

        test("compares every member of a mapping, also those whose keys have the same text") {
            val twins = nodeOf(TWINS)
            val test = { value: YamlNode -> JsonPatch(listOf(JsonPatchOperation.Test(JsonPointer.ROOT, value))) }
            val single = yamlMapping(listOf(YamlEntry(key = yamlScalar("true"), value = yamlScalar(1))))
            val same = test(nodeOf(TWINS))

            same.apply(twins).shouldBeInstanceOf<JsonPatchResult.Applied>()
            test(single).apply(twins) shouldBe JsonPatchResult.Failed(0, JsonPatchRefusal.TestFailed)
            test(nodeOf("{true: 1, 'true': 3}")).apply(twins) shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.TestFailed)
        }
    }

    context("the syntax tree") {
        test("refuses an edit through an alias that the graph does not share") {
            val replace = JsonPatchOperation.Replace("/a/k".toJsonPointer(), yamlScalar(2))

            JsonPatch(listOf(replace)) onTree lines(ALIASED, "b: *x") shouldBe
                JsonPatchEdit.Failed(0, JsonPatchRefusal.Unsupported)
        }

        test("refuses an edit of a duplicate key that the graph resolves to another occurrence") {
            val keepLast = yaml { duplicateKeys = YamlDuplicateKeys.KeepLast }
            val tree = YamlSyntaxTree.parse(lines(SMALL_TREE, "a: 2"), keepLast)
            val replace = JsonPatchOperation.Replace("/a".toJsonPointer(), yamlScalar(THREE))

            JsonPatch(listOf(replace)).apply(tree) shouldBe JsonPatchEdit.Failed(0, JsonPatchRefusal.Unsupported)
        }

        test("leaves a tree unchanged by an empty patch") {
            val edit = JsonPatch(emptyList()) onTree SMALL_TREE
            val edited = edit.shouldBeInstanceOf<JsonPatchEdit.Edited>()

            edited.edits shouldBe emptyList()
            edited.toString() shouldBe "Edited(0 edits)"
        }
    }

    context("merge patches") {
        test("refuse a patch deeper than the depth limit") {
            val shallow = limits { maximumDepth = TWO }
            val deep = JsonMergePatch(nodeOf("{a: {b: {c: 1}}}"), shallow)

            JsonMergePatch(nodeOf("{a: {b: 1}}"), shallow).apply(nodeOf(EMPTY)) shouldBe nodeOf("{a: {b: 1}}")
            shouldThrow<YamlDocumentLimitException> { deep.apply(nodeOf(EMPTY)) }.limit shouldBe "maximumDepth"
        }

        test("count the nodes they add against the node budget") {
            val small = limits { maximumNodes = TWO.toLong() }
            val large = JsonMergePatch(nodeOf(TWO_MEMBERS), small)

            JsonMergePatch(nodeOf(ONE_MEMBER), small).apply(nodeOf(EMPTY)) shouldBe nodeOf(ONE_MEMBER)
            shouldThrow<YamlDocumentLimitException> { large.apply(nodeOf(EMPTY)) }.limit shouldBe "maximumNodes"
        }

        test("merge into the first key with the same text, then into the next one") {
            val sameText = yamlMapping(
                listOf(
                    YamlEntry(key = yamlScalar(1), value = yamlScalar(1)),
                    YamlEntry(key = yamlScalar("1"), value = yamlScalar(2)),
                ),
            )
            val merged = JsonMergePatch(nodeOf("{'1': 3}")).apply(sameText) as YamlNode.Mapping
            val replaced = JsonMergePatch(nodeOf("{1: null, '1': 3}")).apply(sameText)

            merged.entries.map { entry -> entry.value } shouldBe listOf(yamlScalar(THREE), yamlScalar(2))
            replaced shouldBe nodeOf("{'1': 3}")
        }

        test("merge into an equal key written in another form") {
            val merged = JsonMergePatch(nodeOf("{1: b}")).apply(nodeOf("{0x1: a}")) as YamlNode.Mapping

            merged.entries.single().value shouldBe yamlScalar("b")
            merged.entries.single().key shouldBe nodeOf("0x1")
        }

        test("make at most as many changes to a syntax tree as the limits allow") {
            val two = limits { maximumPatchOperations = TWO }
            val within = JsonMergePatch(nodeOf(TWO_MEMBERS), two) onTree "x: 0"

            within.shouldBeInstanceOf<JsonPatchEdit.Edited>()
            JsonMergePatch(nodeOf("{a: 1, b: 2, c: 3}"), two) onTree "x: 0" shouldBe
                JsonPatchEdit.Failed(0, JsonPatchRefusal.LimitExceeded)
        }

        test("refuse a broken document and an edit the editor cannot make") {
            JsonMergePatch(
                nodeOf(ONE_MEMBER),
            ) onTree "a: [1" shouldBe JsonPatchEdit.Failed(0, JsonPatchRefusal.InvalidDocument)
            JsonMergePatch(nodeOf("{a: {b: 2}}")) onTree lines(ALIASED, "a2: *x") shouldBe
                JsonPatchEdit.Failed(0, JsonPatchRefusal.Unsupported)
        }

        test("refuse an edit through an alias that the graph does not share") {
            JsonMergePatch(nodeOf("{a: {k: 2}}")) onTree lines(ALIASED, "b: *x") shouldBe
                JsonPatchEdit.Failed(0, JsonPatchRefusal.Unsupported)
        }
    }

    context("diffs") {
        test("yield at most as many operations as the limits allow") {
            val two = limits { maximumPatchOperations = TWO }

            diff(source = TWO_MEMBERS, target = "{a: 2, b: 3}", limits = two).operations.size shouldBe TWO
            val exceeded = shouldThrow<YamlDocumentLimitException> {
                diff(source = "{a: 1, b: 2, c: 3}", target = "{a: 2, b: 3, c: 4}", limits = two)
            }

            exceeded.limit shouldBe "maximumPatchOperations"
        }

        test("visit at most as many nodes and levels as the limits allow") {
            val shallow = limits { maximumDepth = TWO }
            val few = limits { maximumNodes = THREE.toLong() }

            shouldThrow<YamlDocumentLimitException> {
                diff(
                    source = "[1, 2, 3]",
                    target = "[2, 3, 4]",
                    limits = few,
                )
            }.limit shouldBe "maximumNodes"
            diff(source = "[[x]]", target = "[[y]]", limits = shallow).operations.size shouldBe 1
            shouldThrow<YamlDocumentLimitException> {
                diff(
                    source = "[[[x]]]",
                    target = "[[[y]]]",
                    limits = shallow,
                )
            }.limit shouldBe
                "maximumDepth"
        }
    }

    context("random patches") {
        test("never make reading or applying throw") {
            val document = nodeOf("{a: [1, {b: 2}], b: x}")
            checkAll(patches) { notation ->
                val patch = (JsonPatch.read(notation) as? JsonPatchReading.Read)?.patch
                val written = patch?.toNode()

                patch?.apply(document)
                patch?.apply(treeOf("a: [1, {b: 2}]\nb: x"))
                written?.let(JsonPatch::read) shouldBe patch?.let(JsonPatchReading::Read)
            }
        }
    }
})
