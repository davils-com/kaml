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
package com.davils.kaml.core.compose

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlDuplicateKeys
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.fixture.problems
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kaml.core.yaml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val ALIAS_SIZE: Long = 3L
private const val MERGED_SIZE: Long = 9L
private const val THREE: Int = 3
private const val NESTED_SIZE: Long = 7L

private const val INLINE_MERGE: String = "{<<: {a: 1}}"
private const val MISSING_ALIAS: String = "[&a a, &b b, *missing]"

class ComposeLimitsTest : FunSpec({
    context("anchors") {
        val anchored = "[&a a, &b b]"

        test("are accepted up to the limit") {
            yaml { limits { maximumAnchors = 2 } }.problems(anchored) shouldNotContain YamlDiagnosticCode.TooManyAnchors
        }

        test("stop the composition past the limit") {
            val problems = yaml { limits { maximumAnchors = 1 } }.problems(MISSING_ALIAS)

            problems shouldContain YamlDiagnosticCode.TooManyAnchors
            problems shouldNotContain YamlDiagnosticCode.UndefinedAlias
        }

        test("count a name declared again once") {
            val root = yaml { limits { maximumAnchors = 1 } }.root("[&a a, &a b, *a]")

            root[2].string() shouldBe "b"
        }
    }

    context("aliases") {
        val document = text("a: &a [x, y]", "b: *a")

        test("expand up to the limit") {
            val root = yaml { limits { maximumAliasExpansion = ALIAS_SIZE } }.root(document)

            root["b"][1].string() shouldBe "y"
        }

        test("stop the composition when they expand past the limit") {
            yaml { limits { maximumAliasExpansion = ALIAS_SIZE - 1 } }.problems(document) shouldContain
                YamlDiagnosticCode.TooManyAliasExpansions
        }

        test("count toward the size of their collection") {
            val root = yaml { limits { maximumCollectionSize = THREE } }.root("[&a x, *a, *a]")

            root[2].string() shouldBe "x"
            yaml { limits { maximumCollectionSize = 2 } }.problems("[&a x, *a, *a]") shouldContain
                YamlDiagnosticCode.CollectionTooLarge
        }
    }

    context("merge keys") {
        val merged = text("base: &b {a: 1, b: 2}", "item: {<<: *b}")

        test("charge every merged entry with its nodes up to the limit") {
            val root = legacy(expansion = MERGED_SIZE).root(merged)

            root["item"]["b"].string() shouldBe "2"
        }

        test("stop the composition when merged entries pass the limit") {
            legacy(expansion = MERGED_SIZE - 1).problems(merged) shouldContain YamlDiagnosticCode.TooManyAliasExpansions
        }

        test("charge an inline mapping") {
            val root = legacy(expansion = 2L).root(INLINE_MERGE)

            root["a"].string() shouldBe "1"
            legacy(expansion = 1L).problems(INLINE_MERGE) shouldContain YamlDiagnosticCode.TooManyAliasExpansions
        }

        test("count toward the size of their mapping") {
            val document = "{<<: {a: 1, b: 2}, c: 3}"

            val atLimit = yaml {
                schema = YamlSchema.Yaml11
                limits { maximumCollectionSize = THREE }
            }
            val pastLimit = yaml {
                schema = YamlSchema.Yaml11
                limits { maximumCollectionSize = 2 }
            }

            atLimit.root(document)["b"].string() shouldBe "2"
            pastLimit.problems(document) shouldContain YamlDiagnosticCode.CollectionTooLarge
        }

        test("stop at the first merged entry past the limit") {
            legacy(expansion = 2L).problems("{<<: {a: 1, b: 2, c: 3}}") shouldContain
                YamlDiagnosticCode.TooManyAliasExpansions
        }

        test("charge every node of a nested value") {
            val nested = "{<<: {a: {b: [1, 2, 3]}}}"

            val root = legacy(expansion = NESTED_SIZE).root(nested)

            root["a"]["b"][2].string() shouldBe "3"
            legacy(expansion = 2L).problems(nested) shouldContain YamlDiagnosticCode.TooManyAliasExpansions
        }

        test("keep an explicit key over a merged one") {
            val node = yaml { schema = YamlSchema.Yaml11 }.root("{<<: [{a: 1}, {a: 3, b: 4}], a: 2}")

            node["a"].string() shouldBe "2"
            node["b"].string() shouldBe "4"
            (node as YamlNode.Mapping).entries.size shouldBe 2
        }
    }

    context("duplicate keys") {
        test("leave only the first occurrence in the partial graph when they are refused") {
            val failed = yaml { duplicateKeys = YamlDuplicateKeys.Refuse }.parse(text("a: 1", "b: 2", "a: 3"))
            val partial = failed.shouldBeInstanceOf<YamlParseResult.Failed>().partial
            val root = partial.single?.root.shouldBeInstanceOf<YamlNode.Mapping>()

            root.entries.size shouldBe 2
            root["a"].string() shouldBe "1"
        }

        test("report every repetition") {
            val problems = Yaml.Default.problems(text("a: 1", "a: 2", "a: 3"))

            problems.count { it == YamlDiagnosticCode.DuplicateKey } shouldBe 2
        }

        test("keep the last occurrence in its own place when asked to") {
            val node = yaml { duplicateKeys = YamlDuplicateKeys.KeepLast }.root(
                "{a: 1, b: 2, a: 3}",
            ) as YamlNode.Mapping

            node.keys.map { it.string() } shouldContainExactly listOf("b", "a")
            node["a"].string() shouldBe "3"
        }

        test("describe a collection key by its kind") {
            val result = Yaml.Default.parse(text("? [x]", ": 1", "? [x]", ": 2"))
            val diagnostic = result.diagnostics.single { it.code == YamlDiagnosticCode.DuplicateKey }

            diagnostic.message shouldContain "Sequence"
        }
    }
})

private fun legacy(expansion: Long): Yaml = yaml {
    schema = YamlSchema.Yaml11
    limits { maximumAliasExpansion = expansion }
}
