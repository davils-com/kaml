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
import com.davils.kaml.core.fixture.stream
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.model.YamlVersion
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.int
import com.davils.kaml.core.node.internal.ScalarValue
import com.davils.kaml.core.node.long
import com.davils.kaml.core.node.string
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kaml.core.source.SourceDecoding
import com.davils.kaml.core.source.YamlSource
import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.internal.GreenNode
import com.davils.kaml.core.syntax.internal.GreenToken
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith

private const val LONG_KEY: Int = 1100
private const val SEXAGESIMAL_REAL: Double = -90.5
private const val NEGATIVE_REAL: Double = -1.5
private const val NEGATIVE_THREE: Long = -3L
private const val BEYOND_UNICODE: String = """"\U00110000""""
private const val UNKNOWN_ESCAPE: String = """"\q""""
private const val CRLF: String = "\r\n"
private const val CR: String = "\r"
private const val DOCUMENT_A: String = "--- a"

@OptIn(DavilsInternal::class)
class YamlBranchCasesTest : FunSpec({
    val yaml = Yaml.Default

    context("line breaks inside scalars") {
        test("fold carriage returns in flow scalars") {
            yaml.root(listOf("\"a", "  b", "", "  c\"").joinToString(CRLF)).string() shouldBe "a b\nc"
            yaml.root(listOf("\"a", "  b", "", "  c\"").joinToString(CR)).string() shouldBe "a b\nc"
            yaml.root("plain\r  text").string() shouldBe "plain text"
        }

        test("keep carriage returns out of block scalars") {
            yaml.root(listOf("|", "  a", "  b", "").joinToString(CRLF)).string() shouldBe "a\nb\n"
            yaml.root(listOf(">", "  a", "  b", "").joinToString(CR)).string() shouldBe "a b\n"
        }

        test("keep the empty lines of a keep block scalar") {
            yaml.root("|+\n\n").string() shouldBe "\n"
            yaml.root(text("a: |", "b: 1"))["a"].string() shouldBe ""
        }
    }

    context("unterminated quotes") {
        test("read what is there") {
            yaml.parse("\"").diagnostics.map { it.code } shouldContain YamlDiagnosticCode.UnterminatedScalar
            yaml.parse(""""ab\"""").diagnostics.map { it.code } shouldContain YamlDiagnosticCode.UnterminatedScalar
            yaml.parse("'it''").diagnostics.map { it.code } shouldContain YamlDiagnosticCode.UnterminatedScalar
            yaml.parse("\"ab\\").diagnostics.map { it.code } shouldContain YamlDiagnosticCode.UnterminatedScalar
        }
    }

    context("escapes") {
        test("refuse a code point beyond Unicode") {
            val failed = yaml.parse(BEYOND_UNICODE) as YamlParseResult.Failed

            failed.diagnostics.map { it.code } shouldContain YamlDiagnosticCode.InvalidEscape
            failed.partial.single
                ?.root
                .string() shouldBe "\uFFFD"
        }

        test("keep the letter of an unknown escape") {
            val failed = yaml.parse(UNKNOWN_ESCAPE) as YamlParseResult.Failed

            failed.partial.single
                ?.root
                .string() shouldBe "q"
        }
    }

    context("tags") {
        test("refuse an empty verbatim tag") {
            yaml.problems("!<> a") shouldContain YamlDiagnosticCode.InvalidTag
        }

        test("decode a short or broken percent escape literally") {
            val failed = yaml.parse("!e%2 a") as YamlParseResult.Failed
            val root = failed.partial.documents
                .single()
                .root

            root.tag.uri shouldBe "!e%2"
        }

        test("resolve the tags of collections") {
            yaml.root("! [a]").tag shouldBe YamlTag.SEQUENCE
            yaml.root("!!seq [a]").tag shouldBe YamlTag.SEQUENCE
        }

        test("check a timestamp of YAML 1.1") {
            val legacy = yaml { schema = YamlSchema.Yaml11 }

            legacy.root("!!timestamp 2001-12-14").tag shouldBe YamlTag.TIMESTAMP
            legacy.problems("!!timestamp soon") shouldContain YamlDiagnosticCode.InvalidTaggedValue
            legacy.problems("!!value =") shouldNotContain YamlDiagnosticCode.InvalidTaggedValue
        }

        test("knows no value for an unknown tag") {
            YamlSchema.Json.valueOf(YamlTag("!x"), "a") shouldBe null
            YamlSchema.Yaml11.valueOf(YamlTag("!x"), "a") shouldBe null
            YamlSchema.Failsafe.valueOf(YamlTag.STRING, "a") shouldBe ScalarValue.Text("a")
        }

        test("read YAML 1.1 numbers in every form") {
            val legacy = YamlSchema.Yaml11

            legacy.resolvePlain("-1:30.5") shouldBe YamlTag.FLOAT
            legacy.valueOf(YamlTag.FLOAT, "-1:30.5") shouldBe ScalarValue.Real(SEXAGESIMAL_REAL)
            legacy.valueOf(YamlTag.FLOAT, "-1.5") shouldBe ScalarValue.Real(NEGATIVE_REAL)
            legacy.valueOf(YamlTag.FLOAT, "1.") shouldBe ScalarValue.Real(1.0)
            legacy.valueOf(YamlTag.FLOAT, "1:") shouldBe null
            legacy.valueOf(
                YamlTag.INTEGER,
                "-0b11",
            ) shouldBe ScalarValue.Integer(NEGATIVE_THREE.toString())
        }
    }

    context("directives") {
        test("refuse handles that are no handles") {
            yaml.problems(text("%TAG !e x:", DOCUMENT_A)) shouldContain YamlDiagnosticCode.InvalidDirective
            yaml.problems(text("%TAG !a$! x:", DOCUMENT_A)) shouldContain YamlDiagnosticCode.InvalidDirective
            yaml.problems(text("%TAG x! x:", DOCUMENT_A)) shouldContain YamlDiagnosticCode.InvalidDirective
        }

        test("keep the stream valid with a warning") {
            yaml.events(YamlSource.of(text("%YAML 1.3", DOCUMENT_A))).isValid shouldBe true
        }

        test("read a byte order mark between documents") {
            yaml.stream(text("a", "...", "\uFEFF--- b")).documents.size shouldBe 2
        }
    }

    context("block keys") {
        test("read a JSON-like key with properties and no space") {
            yaml.root("&k \"a\":b")["a"].string() shouldBe "b"
        }

        test("read a quoted key next to its colon") {
            yaml.root("{\"a\":b}")["a"].string() shouldBe "b"
        }
    }

    context("sources") {
        test("detect short inputs") {
            SourceDecoding.encodingOf("\u0000a".encodeToByteArray()).name shouldBe "Utf16BigEndian"
            SourceDecoding.encodingOf("a\u0000b".encodeToByteArray()).name shouldBe "Utf16LittleEndian"
            SourceDecoding.encodingOf("abc".encodeToByteArray()).name shouldBe "Utf8"
        }
    }

    context("node details") {
        test("compare across kinds and to other values") {
            val scalar: Any = yamlScalar("a")
            val sequence: Any = yamlSequence { add("a") }
            val mapping: Any = yamlMapping { "a" to 1 }

            scalar shouldNotBe "a"
            sequence shouldNotBe yamlScalar("a")
            mapping shouldNotBe yamlSequence { add("a") }
            ScalarValue.Real(1.0) shouldNotBe ScalarValue.Integer("1")
        }

        test("find nothing by a string for a key that is no scalar") {
            yaml.root("{[a]: b}")["a"] shouldBe null
        }

        test("convert nothing for a missing node") {
            null.long() shouldBe null
            yaml.root("a").int() shouldBe null
        }

        test("generate a fresh anchor name when a name is taken") {
            val named = yamlSequence(listOf(yamlScalar("x")), anchor = YamlAnchor("id001"))
            val other = yamlSequence { add("y") }
            val written = yaml.emit(yamlSequence(listOf(named, named, other, other)))

            written shouldContain "&id002"
        }
    }

    context("emitting edge cases") {
        test("writes a very long key as a complex key") {
            val key = "k".repeat(LONG_KEY)
            val written = yaml.emit(yamlMapping { key to 1 })

            written shouldStartWith "? "
            yaml.root(written)[key].int() shouldBe 1
        }

        test("quotes strings with characters that plain scalars cannot hold") {
            val values = listOf("a\rb", "tab\tend\t", "\uFFFE", "-[a]", "a:\tb", "\ta")
            val node = yamlSequence(values.map { yamlScalar(it) })

            yaml.root(yaml.emit(node)) shouldBe node
        }

        test("quotes a string in a flow collection that starts like an indicator") {
            val node = yaml.root("[a, b]") as YamlNode.Sequence
            val flow = YamlNode.Sequence(
                items = listOf(yamlScalar("-["), yamlScalar("a,b")),
                tag = node.tag,
                style = node.style,
                anchor = null,
                range = null,
            )

            yaml.root(yaml.emit(flow)) shouldBe flow
        }

        test("writes YAML 1.1 compatible text") {
            val compatible = yaml { emitter { compatibility = YamlVersion.V1_1 } }

            compatible.emit(yaml.root("[12, 1.5, true]")) shouldBe text("[12, 1.5, true]", "")
        }
    }

    context("green elements") {
        val token = GreenToken(YamlSyntaxKind.COMMENT, "# a")
        val node = GreenNode(YamlSyntaxKind.SCALAR, listOf(token))

        test("compare by kind and content") {
            token shouldBe GreenToken(YamlSyntaxKind.COMMENT, "# a")
            token shouldNotBe GreenToken(YamlSyntaxKind.WHITESPACE, "# a")
            token shouldNotBe GreenToken(YamlSyntaxKind.COMMENT, "# b")
            val anyToken: Any = token
            anyToken shouldNotBe "# a"
            token.hashCode() shouldBe GreenToken(YamlSyntaxKind.COMMENT, "# a").hashCode()
            node shouldBe GreenNode(YamlSyntaxKind.SCALAR, listOf(token))
            node shouldNotBe GreenNode(YamlSyntaxKind.ERROR, listOf(token))
            node shouldNotBe GreenNode(YamlSyntaxKind.SCALAR, emptyList())
            node shouldNotBe GreenNode(
                YamlSyntaxKind.SCALAR,
                listOf(GreenToken(YamlSyntaxKind.COMMENT, "# b")),
            )
            val anyNode: Any = node
            anyNode shouldNotBe token
            node.hashCode() shouldBe GreenNode(YamlSyntaxKind.SCALAR, listOf(token)).hashCode()
        }

        test("describe themselves") {
            token.toString() shouldBe "COMMENT(3)"
            node.toString() shouldBe "SCALAR(1 children, 3)"
            node.text shouldBe "# a"
            token.children shouldBe emptyList()
        }

        test("refuse the wrong kind") {
            shouldThrow<IllegalArgumentException> { GreenToken(YamlSyntaxKind.SCALAR, "a") }
            shouldThrow<IllegalArgumentException> { GreenNode(YamlSyntaxKind.COMMENT, emptyList()) }
        }
    }
})
