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
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.fixture.problems
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

private const val KEY_LENGTH: Int = 1030
private const val SUPPLEMENTARY: String = "\uD83D\uDE00"

private val ESCAPED: List<Char> = listOf(
    '\u0000',
    '\u0007',
    '\b',
    '\t',
    '\n',
    '\u000B',
    '\u000C',
    '\r',
    '\u001B',
    ' ',
    '"',
    '/',
    '\\',
    '\u0085',
    '\u00A0',
    '\u2028',
    '\u2029',
    'A',
    '\u00e4',
)

class YamlSyntaxDiagnosticsTest : FunSpec({
    val yaml = Yaml.Default

    val expectations: Map<String, YamlDiagnosticCode> = mapOf(
        "a: \u0001" to YamlDiagnosticCode.InvalidCharacter,
        "a: b: c" to YamlDiagnosticCode.UnexpectedContent,
        "a:\n\t- b" to YamlDiagnosticCode.TabIndentation,
        "a:\n  b: 1\n c: 2" to YamlDiagnosticCode.BadIndentation,
        "a: \"open" to YamlDiagnosticCode.UnterminatedScalar,
        """a: "\q"""" to YamlDiagnosticCode.InvalidEscape,
        """a: "\x4"""" to YamlDiagnosticCode.InvalidEscape,
        text("\"a", "---", "b\"") to YamlDiagnosticCode.DocumentMarkerInScalar,
        text("key:", "  \"a", "b\"") to YamlDiagnosticCode.BadIndentation,
        text("[\"a", "b\": c]") to YamlDiagnosticCode.MultilineImplicitKey,
        "[${"x".repeat(KEY_LENGTH)}: c]" to YamlDiagnosticCode.ImplicitKeyTooLong,
        "a: |++\n  b" to YamlDiagnosticCode.InvalidBlockScalarHeader,
        "a: |0\n  b" to YamlDiagnosticCode.InvalidBlockScalarHeader,
        text("a: |", "  ", "   ", "  b") to YamlDiagnosticCode.InvalidBlockScalarIndentation,
        "a: [b, c" to YamlDiagnosticCode.UnterminatedFlowCollection,
        "a: [b c d}" to YamlDiagnosticCode.MissingFlowSeparator,
        "a: [b, , c]" to YamlDiagnosticCode.UnexpectedContent,
        "a:\n  [b,\nc]" to YamlDiagnosticCode.InvalidFlowIndentation,
        "a: !<open b" to YamlDiagnosticCode.InvalidTag,
        "a: !e%zz b" to YamlDiagnosticCode.InvalidTag,
        "a: & b" to YamlDiagnosticCode.InvalidAnchor,
        "a: * b" to YamlDiagnosticCode.InvalidAnchor,
        "a: &x *y" to YamlDiagnosticCode.AliasWithProperties,
        "a: &x &y b" to YamlDiagnosticCode.DuplicateProperty,
        "%YAML 1.2\na: b" to YamlDiagnosticCode.DirectiveWithoutDocument,
        text("a: |", "  b", "\t", "c: d") to YamlDiagnosticCode.TabIndentation,
        "a: | junk\n  b" to YamlDiagnosticCode.UnexpectedContent,
        "- a\nb: c" to YamlDiagnosticCode.UnexpectedContent,
        "a: 1\nb\nc: 3" to YamlDiagnosticCode.UnexpectedContent,
    )

    context("syntax errors") {
        expectations.forEach { (text, code) ->
            test("reports $code for ${text.replace(oldValue = "\n", newValue = "\\n")}") {
                yaml.problems(text) shouldContain code
            }
        }

        test("reports every problem of a document at once") {
            val problems = yaml.problems(text("""a: "\q"""", "b: & x", "c: [d"))

            problems shouldContain YamlDiagnosticCode.InvalidEscape
            problems shouldContain YamlDiagnosticCode.InvalidAnchor
            problems shouldContain YamlDiagnosticCode.UnterminatedFlowCollection
        }
    }

    context("recovery") {
        test("keeps reading the entries after a broken one") {
            val result = yaml.parse(text("a: [b", "c: d"))

            result.diagnostics.map { it.code } shouldContain YamlDiagnosticCode.UnterminatedFlowCollection
        }

        test("stops at a nesting deeper than the parser allows") {
            yaml.problems("[".repeat(KEY_LENGTH)) shouldContain YamlDiagnosticCode.TooDeep
        }
    }

    context("valid edge cases") {
        test("reads a byte order mark before a document marker") {
            yaml.root("\uFEFF--- a").string() shouldBe "a"
        }

        test("reads carriage return line breaks") {
            yaml.root("a: 1\rb: 2\r\nc: 3")["c"].string() shouldBe "3"
        }

        test("reads escapes of every kind") {
            val value = yaml.root(""""\0\a\b\t\n\v\f\r\e\ \"\/\\\N\_\L\P\x41\u00e4\U0001F600"""").string()

            value shouldBe ESCAPED.joinToString("") + SUPPLEMENTARY
        }

        test("folds escaped line breaks and empty lines in double quotes") {
            yaml.root(text("\"a \\", "  b", "", "  c\"")).string() shouldBe "a b\nc"
        }

        test("reads a single quoted scalar over several lines") {
            yaml.root(text("'a", "  b", "", "  c'")).string() shouldBe "a b\nc"
        }

        test("reads an explicit key without a value") {
            yaml.root(text("? a", "? b"))["a"].string() shouldBe null
        }

        test("reads properties on their own lines") {
            yaml.root(text("&a", "!!str", "value")).string() shouldBe "value"
        }

        test("reads a flow mapping with keys on several lines") {
            yaml.root(text("{ a", "  : b, ? c : d, e }"))["c"].string() shouldBe "d"
        }

        test("reads an empty key in a flow sequence") {
            yaml.root("[: b]")[0][""].string() shouldBe "b"
        }

        test("reads a quoted scalar that continues on a line with tabs") {
            yaml.root(text("\"a", "\tb\"")).string() shouldBe "a b"
        }

        test("reads a reserved directive") {
            yaml.parse(text("%FOO bar baz", "--- a")).diagnostics.map { it.code } shouldContain
                YamlDiagnosticCode.ReservedDirective
        }
    }
})
