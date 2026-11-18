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

package com.davils.kaml.document.diff

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlNull
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.document.fixture.nodeOf
import com.davils.kaml.document.patch.JsonPatch
import com.davils.kaml.document.patch.JsonPatchOperation
import com.davils.kaml.document.patch.JsonPatchResult
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kaml.document.pointer.toJsonPointer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll

private const val MAXIMUM_DEPTH: Int = 3
private const val MAXIMUM_SIZE: Int = 4

private val names = Arb.element("a", "b", "c", "d")

private fun nodes(depth: Int): Arb<YamlNode> {
    val scalars = Arb.choice(
        Arb.int(-3..3).let { arb -> arbitrary { yamlScalar(arb.bind()) } },
        Arb.boolean().let { arb -> arbitrary { yamlScalar(arb.bind()) } },
        arbitrary { yamlScalar(names.bind()) },
        arbitrary { yamlNull() },
    )
    if (depth == 0) return scalars
    val children = nodes(depth - 1)
    val sequences = arbitrary { yamlSequence(Arb.list(children, 0..MAXIMUM_SIZE).bind()) }
    val mappings = arbitrary {
        val chosen = Arb.list(names, 0..MAXIMUM_SIZE).bind()
        val keys = chosen.distinct()
        yamlMapping(keys.map { key -> YamlEntry(key = yamlScalar(key), value = children.bind()) })
    }
    return Arb.choice(scalars, sequences, mappings)
}

class JsonPatchDiffTest : FunSpec({
    context("diff") {
        test("turns the source into the target when it is applied") {
            checkAll(nodes(MAXIMUM_DEPTH), nodes(MAXIMUM_DEPTH)) { source, target ->
                JsonPatch.diff(source = source, target = target).apply(source) shouldBe JsonPatchResult.Applied(target)
            }
        }

        test("is empty for equal documents") {
            JsonPatch.diff(source = nodeOf("{a: [1, 2]}"), target = nodeOf("{a: [1, 2]}")).operations.shouldBeEmpty()
        }

        test("describes the changes with the fewest operations it finds") {
            val patch = JsonPatch.diff(
                source = nodeOf("{a: 1, b: [1, 2, 3], c: x}"),
                target = nodeOf("{a: 2, b: [1], d: y}"),
            )

            patch.operations shouldBe
                listOf(
                    JsonPatchOperation.Remove("/c".toJsonPointer()),
                    JsonPatchOperation.Replace("/a".toJsonPointer(), nodeOf("2")),
                    JsonPatchOperation.Remove("/b/2".toJsonPointer()),
                    JsonPatchOperation.Remove("/b/1".toJsonPointer()),
                    JsonPatchOperation.Add("/d".toJsonPointer(), nodeOf("y")),
                )
        }

        test("replaces mappings whose keys cannot be addressed") {
            JsonPatch.diff(source = nodeOf("{[a]: 1}"), target = nodeOf("{[a]: 2}")).operations shouldBe
                listOf(JsonPatchOperation.Replace("".toJsonPointer(), nodeOf("{[a]: 2}")))
            JsonPatch.diff(source = nodeOf("{1: a}"), target = nodeOf("{1: b}")).operations shouldBe
                listOf(JsonPatchOperation.Replace("".toJsonPointer(), nodeOf("{1: b}")))
        }

        test("replaces a node whose kind changes under the same tag") {
            val source = yamlMapping(listOf(YamlEntry(key = yamlScalar("a"), value = yamlScalar(1))))
            val target = yamlSequence(listOf(yamlScalar(1)), tag = YamlTag.MAPPING)
            val patch = JsonPatch.diff(source = source, target = target)

            patch.operations shouldBe listOf(JsonPatchOperation.Replace(JsonPointer.ROOT, target))
        }

        test("adds the items a sequence gains") {
            JsonPatch.diff(source = nodeOf("[1]"), target = nodeOf("[1, 2]")).operations shouldBe
                listOf(JsonPatchOperation.Add("/-".toJsonPointer(), nodeOf("2")))
        }
    }
})
