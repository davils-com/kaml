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

package com.davils.kaml.document.pointer

import com.davils.kaml.core.YamlDuplicateKeys
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kaml.core.yaml
import com.davils.kaml.document.fixture.lines
import com.davils.kaml.document.fixture.nodeOf
import com.davils.kaml.document.path.JsonPath
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.unsafeCodePointsIn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val LONG: Int = 100_000

private const val PIECES: Int = 32
private const val SHARED_TEXT: String = "1"
private const val FIRST: String = "first"
private const val NESTED: String = "a: {a: 1}"

private val fragments: List<String> = HostileFragments.all + listOf("/", "~", "~0", "~1", "~2")

private val hostile: Arb<String> = Arb.list(Arb.element(fragments), 0..PIECES).map { pieces -> pieces.joinToString("") }

private val duplicated = yamlMapping(
    listOf(
        YamlEntry(key = yamlScalar(1), value = yamlScalar(FIRST)),
        YamlEntry(key = yamlScalar(SHARED_TEXT), value = yamlScalar("second")),
    ),
)

private fun messageOf(failing: () -> Unit): String {
    val failure = runCatching(failing).exceptionOrNull()
    return failure?.message.orEmpty()
}

class JsonPointerHardeningTest : FunSpec({
    context("duplicate keys") {
        test("resolve to their first occurrence by text in a graph, in JSONPath and in a syntax tree") {
            val keepLast = yaml { duplicateKeys = YamlDuplicateKeys.KeepLast }
            val tree = YamlSyntaxTree.parse(lines("a: $FIRST", "a: second"), keepLast)

            val first = yamlScalar(FIRST)
            val matches = JsonPath.parse("$['$SHARED_TEXT']").query(duplicated)
            val found = tree.at("/a".toJsonPointer()).shouldBeInstanceOf<YamlValueSyntax.Scalar>()
            val nearest = tree.nearest("/a/b".toJsonPointer()).shouldBeInstanceOf<YamlValueSyntax.Scalar>()

            duplicated.at("/$SHARED_TEXT".toJsonPointer()) shouldBe first
            matches.single().node shouldBe first
            found.value shouldBe FIRST
            nearest.value shouldBe FIRST
        }
    }

    context("long pointers") {
        test("resolve iteratively, however many tokens they have") {
            val long = JsonPointer.of(List(LONG) { "a" })

            val tree = YamlSyntaxTree.parse(NESTED)
            val parsed = "/a".repeat(LONG).toJsonPointer()

            val node = nodeOf(NESTED)

            node.at(long).shouldBeNull()
            tree.at(long).shouldBeNull()
            tree.nearest(long).shouldBeInstanceOf<YamlValueSyntax.Scalar>()
            parsed.tokens.size shouldBe LONG
        }
    }

    context("hostile tokens") {
        test("round-trip through the RFC 6901 form") {
            checkAll(hostile) { token ->
                val pointer = JsonPointer.of(token, "x")

                pointer.text.toJsonPointer() shouldBe pointer
            }
        }

        test("are shown escaped in every description and message") {
            checkAll(hostile) { token ->
                val message = messageOf { JsonPointer.parse(token) }

                unsafeCodePointsIn(JsonPointer.of(token).toString()).shouldBeEmpty()
                unsafeCodePointsIn(message).shouldBeEmpty()
            }
        }

        test("never make parsing or resolving throw") {
            val node = nodeOf("{a: [1, {b: 2}]}")
            checkAll(hostile) { text ->
                val pointer = text.toJsonPointerOrNull()
                pointer?.let(node::at)
                pointer?.let { YamlSyntaxTree.parse("a: [1]").at(it) }
            }
        }
    }

    context("children") {
        test("refuse a negative index") {
            shouldThrow<IllegalArgumentException> { JsonPointer.ROOT.child(-1) }
            JsonPointer.ROOT.child(0).text shouldBe "/0"
        }

        test("cannot change the tokens of a pointer") {
            val tokens = mutableListOf("a")
            val pointer = JsonPointer.of(tokens)
            tokens.add("b")

            pointer.tokens shouldBe listOf("a")
        }
    }
})
