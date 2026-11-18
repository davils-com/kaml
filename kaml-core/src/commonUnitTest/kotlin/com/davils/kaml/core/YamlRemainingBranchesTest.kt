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

package com.davils.kaml.core

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.fixture.problems
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.model.YamlVersion
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.int
import com.davils.kaml.core.node.string
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kaml.core.schema.yamlSchema
import com.davils.kaml.core.source.YamlSource
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private const val BELOW_INT: String = "-3000000000"
private const val DOCUMENT_A: String = "--- a"

private val coreForms: Map<String, YamlTag> = mapOf(
    "1e+" to YamlTag.STRING,
    ".e5" to YamlTag.STRING,
    "1.5.5" to YamlTag.STRING,
    "+.inf" to YamlTag.FLOAT,
    "0o" to YamlTag.STRING,
    "1.e5" to YamlTag.FLOAT,
)

private val jsonForms: Map<String, YamlTag?> = mapOf(
    "1.5e" to null,
    "0." to YamlTag.FLOAT,
    "1e5x" to null,
    "-01" to null,
    "1e-5" to YamlTag.FLOAT,
)

class YamlRemainingBranchesTest : FunSpec({
    val yaml = Yaml.Default

    context("number forms") {
        coreForms.forEach { (text, tag) ->
            test("the core schema resolves \"$text\" to ${tag.uri}") {
                YamlSchema.Core.resolvePlain(text) shouldBe tag
            }
        }

        jsonForms.forEach { (text, tag) ->
            test("the JSON schema resolves \"$text\" to ${tag?.uri ?: "nothing"}") {
                YamlSchema.Json.resolvePlain(text) shouldBe (tag ?: YamlTag.STRING)
                YamlSchema.Json.refusesPlain(text) shouldBe (tag == null)
            }
        }
    }

    context("nodes") {
        test("compare tagged collections by their tag") {
            yaml.root("!x [a]") shouldNotBe yaml.root("[a]")
            yaml.root("!x {a: 1}") shouldNotBe yaml.root("{a: 1}")
        }

        test("compare a scalar to itself") {
            val scalar = yamlScalar("a")

            (scalar == scalar) shouldBe true
        }

        test("return no int below its range") {
            yaml.root(BELOW_INT).int() shouldBe null
        }

        test("refuse a negative minor version") {
            shouldThrow<IllegalArgumentException> { YamlVersion(major = 1, minor = -1) }
        }
    }

    context("schemas") {
        test("knows a tag that is not implicit") {
            val custom = yamlSchema { sequence("!hosts") }

            custom.definitionOf(YamlTag("!hosts"))?.isImplicit shouldBe false
        }

        test("reports a merge of a scalar") {
            val legacy = yaml { schema = YamlSchema.Yaml11 }

            legacy.problems(text("item:", "  <<: a")) shouldContain YamlDiagnosticCode.InvalidMerge
        }

        test("redefines the primary and secondary handles") {
            val node = yaml.root(text("%TAG ! tag:local,2000:", "%TAG !! tag:global,2000:", "--- [!a x, !!b y]"))

            (node as YamlNode.Sequence).items.map { it.tag.uri } shouldBe
                listOf("tag:local,2000:a", "tag:global,2000:b")
        }
    }

    context("characters") {
        test("reads printable characters of every range") {
            yaml.root("a\u0085b\uE000c").string() shouldBe "a\u0085b\uE000c"
        }

        test("reads a byte order mark inside a line") {
            yaml.parse("a: b\uFEFFc").diagnostics.size shouldBe 0
        }

        test("refuses a directive that is not at the start of its line") {
            yaml.problems(text(" %YAML 1.2", DOCUMENT_A)).isNotEmpty() shouldBe true
        }
    }

    context("emitting") {
        test("refuses an indentation below one") {
            shouldThrow<ValidationException> {
                yaml { emitter { indent = 0 } }
            }
        }

        test("escapes control characters") {
            val node = yamlSequence {
                add("a\u0001b")
                add("c\u009Fd")
                add("e\u0085f")
            }
            val written = yaml.emit(node)

            written shouldContain "\\x01"
            yaml.root(written) shouldBe node
        }

        test("writes anchored events") {
            val events = yaml.events(YamlSource.of(text("- &s [a]", "- &x 'quoted'", "- *x"))).events

            yaml.emitEvents(events) shouldContain "&s"
        }

        test("writes a stream of several explicit documents from events") {
            val events = yaml.events(YamlSource.of(text(DOCUMENT_A, "--- b"))).events

            yaml.emitEvents(events) shouldBe text(DOCUMENT_A, "--- b", "")
        }

        test("writes a document version for older readers") {
            yaml { emitter { compatibility = YamlVersion.V1_1 } }.emit(yamlScalar("text")) shouldBe text("text", "")
        }
    }
})
