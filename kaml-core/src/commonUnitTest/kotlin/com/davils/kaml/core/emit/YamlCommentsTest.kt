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

package com.davils.kaml.core.emit

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.node.YamlDocument
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.unsafeCodePointsIn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val VALUE: String = "value"
private const val SECRET: String = "secret"
private const val FIRST: String = "first"
private const val SMILE: String = "😀"
private const val COMMENT_ITERATIONS: Int = 300
private const val LONGEST_COMMENT: Int = 12

private val refused: Map<String, String> = mapOf(
    "\u0000" to "U+0000",
    "\u0001" to "U+0001",
    "\u001B" to "U+001B",
    "\u007F" to "U+007F",
    "\u0080" to "U+0080",
    "\u0085" to "U+0085",
    "\u009F" to "U+009F",
    "\u00AD" to "U+00AD",
    "\u034F" to "U+034F",
    "\u061C" to "U+061C",
    "\u115F" to "U+115F",
    "\u17B4" to "U+17B4",
    "\u180B" to "U+180B",
    "\u200B" to "U+200B",
    "\u200E" to "U+200E",
    "\u2028" to "U+2028",
    "\u2029" to "U+2029",
    "\u202E" to "U+202E",
    "\u2066" to "U+2066",
    "\u3164" to "U+3164",
    "\uFE0F" to "U+FE0F",
    "\uFEFF" to "U+FEFF",
    "\uFFA0" to "U+FFA0",
    "\uFFF9" to "U+FFF9",
    "\uFFFE" to "U+FFFE",
    "\uFFFF" to "U+FFFF",
    "\uD800" to "U+D800",
    "\uDC00" to "U+DC00",
    "\uDB40\uDC41" to "U+E0041",
    "\uD82F\uDCA0" to "U+1BCA0",
    "\uD834\uDD73" to "U+1D173",
)

private val commentFragments: List<String> = HostileFragments.all + listOf("# ", "\n---\n", ": ", "\r\n")

private val commentText: Arb<String> = Arb.list(Arb.element(commentFragments), 0..LONGEST_COMMENT).map { parts ->
    parts.joinToString(separator = "")
}

class YamlCommentsTest : FunSpec({
    val yaml = Yaml.Default

    context("comments") {
        test("are written above the entries of a mapping") {
            val host = yamlScalar("host")
            val port = yamlScalar("port")
            val root = yamlMapping(
                listOf(
                    YamlEntry(key = host, value = yamlScalar("web")),
                    YamlEntry(key = port, value = yamlScalar("80")),
                ),
            )
            val comments = yamlComments {
                comment(host, "The host\n")
                comment(port, "")
            }

            yaml.emit(
                YamlDocument(root),
                comments,
            ) shouldBe text("# The host", "#", "host: web", "port: \"80\"", "")
        }

        test("are written above a document") {
            val root = yamlScalar(VALUE)
            val comments = yamlComments { comment(root, "A document") }

            yaml.emit(YamlDocument(root), comments) shouldBe text("# A document", VALUE, "")
        }

        test("are written below the marker of an explicit document") {
            val root = yamlMapping(listOf(YamlEntry(key = yamlScalar("a"), value = yamlScalar("b"))))
            val comments = yamlComments { comment(root, "A document") }
            val written = yaml.emit(YamlDocument(root, isExplicitStart = true), comments)

            written shouldBe text("---", "# A document", "a: b", "")
            yaml.root(written) shouldBe root
        }

        test("are written above the later entries of a compact mapping") {
            val second = yamlScalar("b")
            val item = yamlMapping(
                listOf(
                    YamlEntry(key = yamlScalar("a"), value = yamlScalar("1")),
                    YamlEntry(key = second, value = yamlScalar("2")),
                ),
            )
            val comments = yamlComments { comment(second, "Second") }

            yaml.emit(YamlDocument(yamlSequence(listOf(item))), comments) shouldBe
                text("- a: \"1\"", "  # Second", "  b: \"2\"", "")
        }

        test("keep a mapping in a sequence from being compact when its first entry has a comment") {
            val first = yamlScalar("a")
            val item = yamlMapping(listOf(YamlEntry(key = first, value = yamlScalar("1"))))
            val root = yamlSequence(listOf(item))
            val written = yaml.emit(YamlDocument(root), yamlComments { comment(first, "First") })

            written shouldBe text("-", "  # First", "  a: \"1\"", "")
            yaml.root(written) shouldBe root
        }

        test("know no comment for a node without one") {
            YamlComments.None.of(yamlScalar("x")) shouldBe emptyList()
            YamlComments.None.isEmpty shouldBe true
            yamlComments { comment(yamlScalar("x"), "") }.isEmpty shouldBe true
        }

        test("tell how many nodes they mark without showing a comment") {
            val shown = yamlComments { comment(yamlScalar("x"), SECRET) }.toString()

            shown shouldBe "YamlComments(1 nodes)"
            shown shouldNotContain SECRET
        }

        test("split a comment into its lines, whatever line breaks it uses") {
            val root = yamlScalar(VALUE)
            val text = listOf("one", "\r\n", "two", "\r", "three", "\n", "four", "\r\n\r\n", "six", "\r").joinToString(
                "",
            )
            val comments = yamlComments { comment(root, text) }

            comments.of(root) shouldBe listOf("one", "two", "three", "four", "", "six", "")
        }

        test("add the lines of a later comment to those of an earlier one") {
            val root = yamlScalar(VALUE)
            val comments = yamlComments {
                comment(root, FIRST)
                comment(root, "second")
            }

            comments.of(root) shouldBe listOf(FIRST, "second")
        }

        test("keep tabs and a pair of surrogates, which is a single character") {
            val root = yamlScalar(VALUE)

            yamlComments { comment(root, "a\tb $SMILE") }.of(root) shouldBe listOf("a\tb $SMILE")
        }

        test("refuse every character that could break the YAML or hide text, naming only its code point") {
            val root = yamlScalar(VALUE)

            refused.forEach { (character, notation) ->
                val failure = shouldThrow<IllegalArgumentException> {
                    yamlComments { comment(root, "ok\n$SECRET$character") }
                }
                failure.message.orEmpty() shouldContain notation
                failure.message.orEmpty() shouldNotContain SECRET
            }
        }

        test("cannot be changed through the lists they return") {
            val root = yamlScalar(VALUE)
            val builder = YamlCommentsBuilder()
            builder.comment(root, FIRST)
            val comments = builder.build()
            builder.comment(root, "later")

            comments.of(root) shouldBe listOf(FIRST)
            shouldThrow<UnsupportedOperationException> { (comments.of(root) as MutableList<String>).add("x") }
        }

        test("never let a hostile comment change the document") {
            val key = yamlScalar("key")
            val root = yamlMapping(listOf(YamlEntry(key = key, value = yamlScalar(VALUE))))
            checkAll(COMMENT_ITERATIONS, commentText) { text ->
                acceptedOrRefused { yamlComments { comment(key, text) } }?.let { comments ->
                    shouldKeepDocument(yaml, root, comments)
                }
            }
        }
    }
})

private fun acceptedOrRefused(build: () -> YamlComments): YamlComments? = try {
    build()
} catch (refusal: IllegalArgumentException) {
    refusal.message.orEmpty() shouldStartWith "A comment cannot contain U+"
    null
}

private fun shouldKeepDocument(yaml: Yaml, root: YamlNode, comments: YamlComments) {
    val written = yaml.emit(YamlDocument(root), comments)
    withClue(written) {
        yaml.root(written) shouldBe root
        written.lines().forEach { line -> unsafeCodePointsIn(line, allowTabs = true).shouldBeEmpty() }
    }
}
