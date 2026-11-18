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
import com.davils.kaml.core.fixture.stream
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.model.YamlTagDirective
import com.davils.kaml.core.model.YamlVersion
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kaml.core.yaml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val SMALL: Int = 3
private const val ITEM: String = "item:"
private const val YAML_DIRECTIVE: String = "%YAML 1.2"
private const val LAUGHS: Int = 9

class YamlCompositionRulesTest : FunSpec({
    val yaml = Yaml.Default

    context("anchors and aliases") {
        test("share the anchored node") {
            val node = yaml.root(text("base: &b {a: 1}", "copy: *b"))

            node["copy"] shouldBeSameInstanceAs node["base"]
        }

        test("refer to the most recent anchor of a name") {
            val node = yaml.root(text("- &x one", "- &x two", "- *x"))

            node[2].string() shouldBe "two"
        }

        test("report an alias without an anchor") {
            yaml.problems("a: *missing") shouldContain YamlDiagnosticCode.UndefinedAlias
        }

        test("report an alias to the node that contains it") {
            yaml.problems("&loop [*loop]") shouldContain YamlDiagnosticCode.RecursiveAlias
        }

        test("read an alias to an inner anchor of the same name") {
            val node = yaml.root("&a [&a b, *a]")

            node[1].string() shouldBe "b"
        }

        test("forget anchors between documents") {
            yaml.problems(text("&a one", "---", "*a")) shouldContain YamlDiagnosticCode.UndefinedAlias
        }
    }

    context("duplicate keys") {
        val document = text("a: 1", "a: 2")

        test("are refused by default") {
            yaml.problems(document) shouldContain YamlDiagnosticCode.DuplicateKey
        }

        test("keep the first value when asked to") {
            val first = yaml { duplicateKeys = YamlDuplicateKeys.KeepFirst }.root(document)

            first["a"].string() shouldBe "1"
        }

        test("keep the last value when asked to") {
            val last = yaml { duplicateKeys = YamlDuplicateKeys.KeepLast }.root(document)

            last["a"].string() shouldBe "2"
        }

        test("are recognised for keys that are collections") {
            yaml.problems(text("? [a]", ": 1", "? [a]", ": 2")) shouldContain YamlDiagnosticCode.DuplicateKey
        }
    }

    context("merge keys") {
        val legacy = yaml { schema = YamlSchema.Yaml11 }

        test("merge a mapping without overriding explicit keys") {
            val node = legacy.root(text("base: &b {a: 1, b: 2}", ITEM, "  b: 3", "  <<: *b"))

            node["item"]["a"].string() shouldBe "1"
            node["item"]["b"].string() shouldBe "3"
        }

        test("merge a sequence of mappings in order") {
            val node = legacy.root(text("one: &one {a: 1}", "two: &two {a: 2, b: 2}", ITEM, "  <<: [*one, *two]"))

            node["item"]["a"].string() shouldBe "1"
            node["item"]["b"].string() shouldBe "2"
        }

        test("report a merge of something that is no mapping") {
            legacy.problems(text(ITEM, "  <<: [a]")) shouldContain YamlDiagnosticCode.InvalidMerge
        }

        test("are plain keys in the core schema") {
            yaml.root(text(ITEM, "  <<: {a: 1}"))["item"]["<<"]["a"].string() shouldBe "1"
        }
    }

    context("limits") {
        test("refuse an input that is too long") {
            val tiny = yaml { limits { maximumInputLength = SMALL } }

            tiny.problems("abcdef") shouldContain YamlDiagnosticCode.InputTooLong
        }

        test("refuse a document that nests too deeply") {
            val shallow = yaml { limits { maximumDepth = SMALL } }

            shallow.problems("[[[[[a]]]]]") shouldContain YamlDiagnosticCode.TooDeep
            shallow.problems(
                text("a:", " b:", "  c:", "   d:", "    e: f"),
            ) shouldContain YamlDiagnosticCode.TooDeep
        }

        test("refuse too many nodes") {
            yaml { limits { maximumNodes = SMALL.toLong() } }.problems("[a, b, c, d]") shouldContain
                YamlDiagnosticCode.TooManyNodes
        }

        test("refuse a billion laughs") {
            val laughs = (1..LAUGHS).joinToString("\n") { level ->
                val previous = "*l${level - 1}"
                "l$level: &l$level [$previous, $previous, $previous, $previous, $previous]"
            }
            val document = text("l0: &l0 lol", laughs)

            yaml.problems(document) shouldContain YamlDiagnosticCode.TooManyAliasExpansions
        }

        test("refuse too many anchors") {
            yaml { limits { maximumAnchors = 1 } }.problems(
                "[&a a, &b b]",
            ) shouldContain YamlDiagnosticCode.TooManyAnchors
        }

        test("refuse a scalar that is too long") {
            yaml { limits { maximumScalarLength = SMALL } }.problems(
                "abcdef",
            ) shouldContain YamlDiagnosticCode.ScalarTooLong
        }

        test("refuse a collection that is too large") {
            yaml { limits { maximumCollectionSize = 1 } }.problems("[a, b]") shouldContain
                YamlDiagnosticCode.CollectionTooLarge
        }

        test("refuse too many documents") {
            yaml { limits { maximumDocuments = 1 } }.problems(text("a", "---", "b")) shouldContain
                YamlDiagnosticCode.TooManyDocuments
        }

        test("keep a partial stream when composing fails") {
            val failed = yaml.parse(text("a: *missing")).shouldBeInstanceOf<YamlParseResult.Failed>()

            failed.partial.documents.size shouldBe 1
        }
    }

    context("directives") {
        test("describe the document they belong to") {
            val stream = yaml.stream(text(YAML_DIRECTIVE, "%TAG !e! tag:example.com,2000:", "--- !e!foo bar"))
            val document = stream.documents.single()

            document.version shouldBe YamlVersion.V1_2
            document.tagDirectives shouldBe listOf(YamlTagDirective(handle = "!e!", prefix = "tag:example.com,2000:"))
            document.root.tag.uri shouldBe "tag:example.com,2000:foo"
            document.isExplicitStart shouldBe true
        }

        test("report a malformed %YAML directive") {
            yaml.problems(text("%YAML 1", "---", "a")) shouldContain YamlDiagnosticCode.InvalidDirective
            yaml.problems(text("%YAML x.y", "---", "a")) shouldContain YamlDiagnosticCode.InvalidDirective
        }

        test("report a %YAML directive twice") {
            yaml.problems(
                text(YAML_DIRECTIVE, YAML_DIRECTIVE, "---", "a"),
            ) shouldContain YamlDiagnosticCode.DuplicateDirective
        }

        test("refuse a major version other than 1") {
            yaml.problems(text("%YAML 2.0", "---", "a")) shouldContain YamlDiagnosticCode.UnsupportedVersion
        }

        test("warn about a newer minor version") {
            val result = yaml.parse(text("%YAML 1.3", "---", "a"))

            result.diagnostics.map { it.code } shouldContain YamlDiagnosticCode.NewerMinorVersion
            result.shouldBeInstanceOf<YamlParseResult.Parsed>()
        }

        test("report a malformed %TAG directive") {
            yaml.problems(text("%TAG e tag:x,2000:", "---", "a")) shouldContain YamlDiagnosticCode.InvalidDirective
            yaml.problems(text("%TAG !e!", "---", "a")) shouldContain YamlDiagnosticCode.InvalidDirective
        }

        test("report a %TAG handle twice") {
            yaml.problems(
                text("%TAG !e! a:", "%TAG !e! b:", "---", "a"),
            ) shouldContain YamlDiagnosticCode.DuplicateDirective
        }

        test("warn about a reserved directive") {
            yaml.problems(text("%FOO bar", "---", "a")) shouldContain YamlDiagnosticCode.ReservedDirective
        }

        test("report an undeclared tag handle") {
            yaml.problems("!e!foo bar") shouldContain YamlDiagnosticCode.UnknownTagHandle
        }

        test("report a verbatim tag without its closing bracket") {
            yaml.problems("!<\n a") shouldContain YamlDiagnosticCode.InvalidTag
            yaml.problems("!<x a") shouldContain YamlDiagnosticCode.InvalidTag
        }

        test("decode a percent escape in a tag") {
            yaml.root("!e%21x bar").tag.uri shouldBe "!e!x"
            yaml.root("!<tag:example.com,2000:x> bar").tag.uri shouldBe "tag:example.com,2000:x"
            yaml.root("!e%C3%A4 bar").tag.uri shouldBe "!eä"
        }
    }

    context("empty documents") {
        test("read as a null value") {
            val node = yaml
                .stream("---")
                .documents
                .single()
                .root

            node.shouldBeInstanceOf<YamlNode.Scalar>()
            yaml.problems("---") shouldNotContain YamlDiagnosticCode.UnexpectedContent
        }
    }
})
