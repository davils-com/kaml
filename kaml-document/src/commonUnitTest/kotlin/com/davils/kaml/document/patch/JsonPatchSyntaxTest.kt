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

import com.davils.kaml.document.fixture.lines
import com.davils.kaml.document.fixture.nodeOf
import com.davils.kaml.document.fixture.treeOf
import com.davils.kaml.document.pointer.JsonPointer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private const val HEADER: String = "# servers of the lab"
private const val SERVER: String = "server:"
private const val PORT: String = "  port: 8080 # default"
private const val HOST: String = "  host: a"
private const val ITEMS: String = "items:"
private const val ADD_ROOT: String = "[{op: add, path: '', value: x}]"
private const val ITEM_X: String = "  - x"
private const val ITEM_Y: String = "  - y"

private val config = lines(HEADER, SERVER, PORT, HOST, ITEMS, ITEM_X, ITEM_Y)

class JsonPatchSyntaxTest : FunSpec({
    context("applying a patch to the syntax tree") {
        test("replaces a value and keeps the comments") {
            patchedText(text = config, patch = "[{op: replace, path: /server/port, value: 9090}]") shouldBe
                lines(HEADER, SERVER, "  port: 9090 # default", HOST, ITEMS, ITEM_X, ITEM_Y)
        }

        test("adds members and items where the indices point") {
            patchedText(
                text = config,
                patch = "[{op: add, path: /server/name, value: n}, {op: add, path: /items/1, value: z}, " +
                    "{op: add, path: /items/-, value: w}]",
            ) shouldBe lines(HEADER, SERVER, PORT, HOST, "  name: n", ITEMS, ITEM_X, "  - z", ITEM_Y, "  - w")
        }

        test("removes members and items") {
            patchedText(
                text = config,
                patch = "[{op: remove, path: /server/host}, {op: remove, path: /items/0}]",
            ) shouldBe
                lines(HEADER, SERVER, PORT, ITEMS, ITEM_Y)
        }

        test("moves and copies values") {
            patchedText(text = config, patch = "[{op: move, from: /server/host, path: /host}]") shouldBe
                lines(HEADER, SERVER, PORT, ITEMS, ITEM_X, ITEM_Y, "host: a")
            patchedText(text = config, patch = "[{op: copy, from: /items/0, path: /items/-}]") shouldBe
                lines(HEADER, SERVER, PORT, HOST, ITEMS, ITEM_X, ITEM_Y, ITEM_X)
        }

        test("changes nothing for a passing test") {
            val edit = patchedSyntax(
                text = config,
                patch = "[{op: test, path: /server/host, value: a}]",
            ) as JsonPatchEdit.Edited

            edit.edits.shouldBeEmpty()
            edit.tree.text shouldBe config
        }

        test("replaces the whole document") {
            patchedText(
                text = lines("a: 1", "b: 2"),
                patch = "[{op: replace, path: '', value: [1, 2]}]",
            ) shouldBe "[1, 2]"
            patchedText(text = "a: 1 # note", patch = ADD_ROOT) shouldBe "x # note"
        }

        test("edits the selected document") {
            patchedText(
                text = lines("a: 1", "---", "b: 2"),
                patch = "[{op: replace, path: /b, value: 3}]",
                document = 1,
            ) shouldBe
                lines("a: 1", "---", "b: 3")
        }

        test("returns a single minimal edit") {
            val edit = patchedSyntax(
                text = config,
                patch = "[{op: replace, path: /server/host, value: b}]",
            ) as JsonPatchEdit.Edited

            edit.edits.single().replacement shouldBe "b"
        }
    }

    context("failures") {
        test("name the operation the graph refuses") {
            patchedSyntax(text = config, patch = "[{op: test, path: /a, value: 1}, {op: remove, path: /x}]") shouldBe
                JsonPatchEdit.Failed(0, JsonPatchRefusal.MissingTarget)
        }

        test("name an operation the editor cannot express") {
            patchedSyntax(text = lines("a: &x [1]", "b: *x"), patch = "[{op: add, path: /b/-, value: 2}]") shouldBe
                JsonPatchEdit.Failed(0, JsonPatchRefusal.Unsupported)
        }

        test("refuse a missing or invalid document") {
            patchedSyntax(text = "a: 1", patch = "[{op: remove, path: /a}]", document = 1) shouldBe
                JsonPatchEdit.Failed(0, JsonPatchRefusal.MissingDocument)
            patchedSyntax(text = "a: [1", patch = "[{op: remove, path: /a}]") shouldBe
                JsonPatchEdit.Failed(0, JsonPatchRefusal.InvalidDocument)
            val unchanged = JsonPatch(emptyList()).apply(treeOf("a: 1")) as JsonPatchEdit.Edited

            unchanged.edits.shouldBeEmpty()
            unchanged.tree.text shouldBe "a: 1"
        }

        test("refuse to replace the root of a missing document") {
            val patch = JsonPatch(listOf(JsonPatchOperation.Replace(JsonPointer.ROOT, nodeOf("1"))))

            patch.apply(
                treeOf("a: 1"),
                document = 3,
            ) shouldBe JsonPatchEdit.Failed(0, JsonPatchRefusal.MissingDocument)
        }
    }

    context("edges") {
        test("addresses mapping keys that look like indices") {
            patchedText(
                text = lines("ports:", "  80: http"),
                patch = "[{op: replace, path: /ports/80, value: https}]",
            ) shouldBe
                lines("ports:", "  80: https")
        }

        test("keeps the properties of a replaced root and fills an empty document") {
            patchedText(text = "&a {x: 1}", patch = "[{op: replace, path: '', value: [2]}]") shouldBe "&a [2]"
            patchedText(text = "---\n", patch = ADD_ROOT) shouldBe "--- x\n"
            patchedText(text = "--- \n", patch = ADD_ROOT) shouldBe "--- x\n"
        }

        test("refuses a move the editor cannot remove") {
            patchedSyntax(text = lines("- a: 1", "  b: 2"), patch = "[{op: move, from: /0/a, path: /0/c}]") shouldBe
                JsonPatchEdit.Failed(0, JsonPatchRefusal.Unsupported)
        }
    }
})
