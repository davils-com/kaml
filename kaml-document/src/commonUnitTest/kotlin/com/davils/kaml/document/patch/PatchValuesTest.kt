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

import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.document.fixture.treeOf
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val first = JsonPointer.of("a")
private val second = JsonPointer.of("b")
private val one = yamlScalar(1)
private val two = yamlScalar(2)

private fun distinct(vararg values: Any): Boolean {
    val pairs = values.indices.flatMap { left -> values.indices.filter { it != left }.map { right -> left to right } }
    return pairs.none { (left, right) -> values[left] == values[right] }
}

class PatchValuesTest : FunSpec({
    context("operations") {
        test("are equal by kind, location and value") {
            JsonPatchOperation.Add(first, one) shouldBe JsonPatchOperation.Add(first, one)
            JsonPatchOperation.Add(first, one).hashCode() shouldBe JsonPatchOperation.Add(first, one).hashCode()
            JsonPatchOperation.Remove(first).hashCode() shouldBe JsonPatchOperation.Remove(first).hashCode()
            JsonPatchOperation.Replace(first, one).hashCode() shouldBe JsonPatchOperation.Replace(first, one).hashCode()
            val move = JsonPatchOperation.Move(from = first, path = second)
            val copy = JsonPatchOperation.Copy(from = first, path = second)

            move.hashCode() shouldBe JsonPatchOperation.Move(from = first, path = second).hashCode()
            copy.hashCode() shouldBe JsonPatchOperation.Copy(from = first, path = second).hashCode()
            JsonPatchOperation.Test(first, one).hashCode() shouldBe JsonPatchOperation.Test(first, one).hashCode()
            distinct(
                JsonPatchOperation.Add(first, one),
                JsonPatchOperation.Add(second, one),
                JsonPatchOperation.Add(first, two),
                JsonPatchOperation.Remove(first),
                JsonPatchOperation.Remove(second),
                JsonPatchOperation.Replace(first, one),
                JsonPatchOperation.Replace(second, one),
                JsonPatchOperation.Replace(first, two),
                JsonPatchOperation.Move(from = first, path = second),
                JsonPatchOperation.Move(from = second, path = second),
                JsonPatchOperation.Move(from = first, path = first),
                JsonPatchOperation.Copy(from = first, path = second),
                JsonPatchOperation.Copy(from = second, path = second),
                JsonPatchOperation.Copy(from = first, path = first),
                JsonPatchOperation.Test(first, one),
                JsonPatchOperation.Test(second, one),
                JsonPatchOperation.Test(first, two),
            ) shouldBe true
        }
    }

    context("results") {
        test("are equal by their content") {
            val edits = listOf(TextEdit(TextRange.at(0), "x"))
            val tree = treeOf("a: 1")

            JsonPatchResult.Applied(one) shouldBe JsonPatchResult.Applied(yamlScalar(1))
            JsonPatchResult.Applied(one).hashCode() shouldBe one.hashCode()
            JsonPatchResult.Applied(one).toString() shouldBe "Applied(Scalar)"
            JsonPatchResult.Failed(1, JsonPatchRefusal.TestFailed).hashCode() shouldBe
                JsonPatchResult.Failed(1, JsonPatchRefusal.TestFailed).hashCode()
            JsonPatchEdit.Edited(edits, tree) shouldBe JsonPatchEdit.Edited(edits, treeOf("a: 1"))
            JsonPatchEdit.Edited(edits, tree).hashCode() shouldBe JsonPatchEdit.Edited(edits, tree).hashCode()
            JsonPatchEdit.Failed(0, JsonPatchRefusal.Unsupported).toString() shouldBe "Failed(0, Unsupported)"
            JsonPatchEdit.Failed(0, JsonPatchRefusal.Unsupported).hashCode() shouldBe
                JsonPatchEdit.Failed(0, JsonPatchRefusal.Unsupported).hashCode()
            JsonPatchResult.Failed(0, JsonPatchRefusal.Unsupported).toString() shouldBe "Failed(0, Unsupported)"
            distinct(
                JsonPatchResult.Applied(one),
                JsonPatchResult.Applied(two),
                JsonPatchResult.Failed(0, JsonPatchRefusal.TestFailed),
                JsonPatchResult.Failed(1, JsonPatchRefusal.TestFailed),
                JsonPatchResult.Failed(0, JsonPatchRefusal.MissingTarget),
                JsonPatchEdit.Edited(edits, tree),
                JsonPatchEdit.Edited(emptyList(), tree),
                JsonPatchEdit.Edited(edits, treeOf("b: 1")),
                JsonPatchEdit.Failed(0, JsonPatchRefusal.TestFailed),
                JsonPatchEdit.Failed(1, JsonPatchRefusal.TestFailed),
                JsonPatchEdit.Failed(0, JsonPatchRefusal.MissingTarget),
            ) shouldBe true
        }

        test("of reading are equal by their content") {
            val patch = JsonPatch(listOf(JsonPatchOperation.Remove(first)))
            val read = JsonPatchReading.Read(patch)

            read shouldBe JsonPatchReading.Read(JsonPatch(listOf(JsonPatchOperation.Remove(first))))
            read.hashCode() shouldBe patch.hashCode()
            read.toString() shouldBe "Read(JsonPatch(1 operations))"
            JsonPatchReading.Invalid(0, "x").toString() shouldBe "Invalid(0, x)"
            JsonPatchReading.Invalid(0, "x").hashCode() shouldBe JsonPatchReading.Invalid(0, "x").hashCode()
            distinct(
                read,
                JsonPatchReading.Read(JsonPatch(emptyList())),
                JsonPatchReading.Invalid(0, "x"),
                JsonPatchReading.Invalid(1, "x"),
                JsonPatchReading.Invalid(0, "y"),
            ) shouldBe true
        }
    }
})
