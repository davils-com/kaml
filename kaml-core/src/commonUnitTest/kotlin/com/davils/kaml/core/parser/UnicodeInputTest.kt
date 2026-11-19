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
package com.davils.kaml.core.parser

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.fixture.problems
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kore.core.text.TextRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val SUPPLEMENTARY: String = "😀"
private const val CONTENT_START: Int = 4
private const val PROPERTY_LINES: Int = 20_000

private val disallowed: Map<String, String> = mapOf(
    "a C0 control" to "\u0001",
    "a delete" to "\u007F",
    "a C1 control" to "\u0080",
    "the last C1 control" to "\u009F",
    "a lone high surrogate" to "\uD800",
    "a lone low surrogate" to "\uDC00",
    "a reversed surrogate pair" to "\uDE00\uD83D",
    "the noncharacter U+FFFE" to "\uFFFE",
    "the noncharacter U+FFFF" to "\uFFFF",
)

private val validEscapes: Map<String, String> = mapOf(
    "\\uD83D\\uDE00" to SUPPLEMENTARY,
    "\\U0001F600" to SUPPLEMENTARY,
    "\\x41\\u00e4" to "Aä",
    "\\U0010FFFF" to "\uDBFF\uDFFF",
)

private val invalidEscapes: List<String> = listOf(
    "\\uD800",
    "\\uDC00",
    "\\uDE00\\uD83D",
    "\\uD83Dx",
    "\\uD83D\\u0041",
    "\\uD83D\\uD83D",
    "\\U0000D800",
    "\\U00110000",
    "\\UFFFFFFFF",
    "\\uD83D\\u12",
    "\\uD83D\\uXYZW",
    "\\uD83D\\x41",
    "\\u+123",
)

class UnicodeInputTest : FunSpec({
    val yaml = Yaml.Default

    context("characters that YAML does not allow") {
        disallowed.forEach { (name, characters) ->
            test("refuse $name") {
                val result = yaml.parse("a: b${characters}c").shouldBeInstanceOf<YamlParseResult.Failed>()
                val diagnostic = result.diagnostics.single { it.code == YamlDiagnosticCode.InvalidCharacter }

                diagnostic.range shouldBe TextRange(start = CONTENT_START, end = CONTENT_START + characters.length)
            }
        }

        test("are reported once for a run of them") {
            val problems = yaml.problems("a: \u0001\u0002\u0003")

            problems.count { it == YamlDiagnosticCode.InvalidCharacter } shouldBe 1
        }

        test("leave the printable characters alone") {
            val value = "\t\u0085\u00A0\uD7FF\uE000\uFFFD$SUPPLEMENTARY"

            yaml.root("\"$value\"").string() shouldBe value
        }

        test("never let U+FFFF end the input") {
            val source = text("a: \"b\uFFFFc\"", "d: e")
            val failed = yaml.parse(source).shouldBeInstanceOf<YamlParseResult.Failed>()
            val root = failed.partial.single?.root

            root["a"].string() shouldBe "b\uFFFFc"
            root["d"].string() shouldBe "e"
            SyntaxParser.parse(source).root.text shouldBe source
        }

        test("never let U+FFFF end a quoted scalar") {
            yaml.problems("\"b\uFFFFc\"") shouldNotContain YamlDiagnosticCode.UnterminatedScalar
            yaml.problems("[\uFFFF]") shouldNotContain YamlDiagnosticCode.UnterminatedFlowCollection
        }
    }

    context("escapes") {
        validEscapes.forEach { (escape, value) ->
            test("decode $escape") {
                yaml.root("\"$escape\"").string() shouldBe value
            }
        }

        invalidEscapes.forEach { escape ->
            test("refuse $escape, which is no Unicode scalar value") {
                val failed = yaml.parse("\"$escape\"").shouldBeInstanceOf<YamlParseResult.Failed>()
                val value = failed.partial.single
                    ?.root
                    .string()
                    .orEmpty()

                failed.diagnostics.map { it.code } shouldContain YamlDiagnosticCode.InvalidEscape
                value.indices.none { index -> value.isLoneSurrogateAt(index) } shouldBe true
            }
        }
    }

    context("truncated escapes") {
        test("end at the end of the input") {
            listOf("\"\\u1", "\"\\U0001F6", "\"\\x").forEach { source ->
                SyntaxParser.parse(source).root.text shouldBe source
                yaml.problems(source) shouldContain YamlDiagnosticCode.InvalidEscape
            }
        }

        test("leave the closing quote alone") {
            yaml.problems(""""\x4"""") shouldNotContain YamlDiagnosticCode.UnterminatedScalar
        }
    }

    context("tags") {
        test("refuse percent escapes that are no UTF-8") {
            yaml.problems("!e%FF x") shouldContain YamlDiagnosticCode.InvalidTag
            yaml.problems("!e%C3 x") shouldContain YamlDiagnosticCode.InvalidTag
        }

        test("leave the percent escapes of a verbatim tag alone") {
            yaml.problems("!<tag:x,2026:%FF> y") shouldNotContain YamlDiagnosticCode.InvalidTag
        }
    }

    context("properties on later lines") {
        test("are read without a recursion for each line") {
            val lines = List(PROPERTY_LINES) { "  !t" }
            val document = (listOf("key:") + lines + "  value").joinToString("\n")

            yaml.problems(document) shouldContain YamlDiagnosticCode.DuplicateProperty
        }
    }
})

private fun String.isLoneSurrogateAt(index: Int): Boolean {
    val character = this[index]
    val isPairedHigh = character.isHighSurrogate() && getOrNull(index + 1)?.isLowSurrogate() == true
    val isPairedLow = character.isLowSurrogate() && getOrNull(index - 1)?.isHighSurrogate() == true
    return character.isSurrogate() && !isPairedHigh && !isPairedLow
}
