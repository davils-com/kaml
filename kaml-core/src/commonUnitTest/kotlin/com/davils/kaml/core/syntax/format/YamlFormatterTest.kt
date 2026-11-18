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

package com.davils.kaml.core.syntax.format

import com.davils.kaml.core.fixture.lines
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.fixture.deepMappings
import com.davils.kaml.core.yaml
import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.text.edited
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val WIDE: Int = 4
private const val TOO_WIDE: Int = 10
private const val WIDE_B: String = "    b: 1"
private const val NARROW_B: String = "  b: 1"
private const val TRAILING_A: String = "a: 1  "
private const val TRAILING_B: String = "b: 2  "
private const val TRIPLE: Int = 3
private const val DEFAULT_INDENT: String = "indent=2"
private const val SMALL_INPUT: Int = 8
private const val WIDEST: Int = 9
private val NARROW_DEEP: String = lines("a:", " b:", "  c: 1", "")
private val WIDE_DEEP: String = lines("a:", "    b:", "        c: 1", "")
private const val DEEP_KEYS: Int = 100

class YamlFormatterTest : FunSpec({
    context("indentation") {
        test("reindents nested mappings") {
            formatted(
                lines("a:", WIDE_B, "    c:", "        d: 2"),
            ) shouldBe lines("a:", NARROW_B, "  c:", "    d: 2", "")
        }

        test("uses the configured indent") {
            val options = yamlFormatOptions { indent = WIDE }

            formatted(lines("a:", NARROW_B), options) shouldBe lines("a:", WIDE_B, "")
        }

        test("indents sequences below their key") {
            formatted(lines("a:", "- x", "- y")) shouldBe lines("a:", "  - x", "  - y", "")
        }

        test("keeps sequences at the column of their key when asked to") {
            val options = yamlFormatOptions { isSequenceIndented = false }

            formatted(lines("a:", "    - x", "    - y"), options) shouldBe lines("a:", "- x", "- y", "")
        }

        test("indents the content of sequence items") {
            formatted(lines("-   a: 1", "    b: 2", "-     - x")) shouldContain "- a: 1"
        }

        test("keeps the line breaks of the document") {
            formatted("a:\r\n    b: 1") shouldBe "a:\r\n  b: 1\r\n"
        }

        test("formats compact collections below explicit keys") {
            formatted(lines("? a: 1", "  b: 2", ": c")) shouldBe lines("? a: 1", "  b: 2", ": c", "")
        }

        test("formats streams with markers between documents") {
            formatted(
                lines("", "a: 1", "...", "...", "---", "b:", "    c: 2"),
            ) shouldBe lines("", "a: 1", "...", "...", "---", "b:", "  c: 2", "")
        }

        test("moves comments with the line below them") {
            formatted(lines("a:", "     # note", WIDE_B)) shouldBe lines("a:", "  # note", NARROW_B, "")
        }

        test("shifts block scalars with their key and keeps their value") {
            formatted(lines("a:", "    b: |", "      first", "        second", "    c: 1")) shouldBe
                lines("a:", "  b: |", "    first", "      second", "  c: 1", "")
        }
    }

    context("whitespace") {
        test("removes trailing whitespace") {
            formatted(lines(TRAILING_A, "b: 2\t")) shouldBe lines("a: 1", "b: 2", "")
        }

        test("collapses runs of blank lines") {
            formatted(lines("a: 1", "", "  ", "", "b: 2")) shouldBe lines("a: 1", "", "b: 2", "")
        }

        test("keeps the configured number of blank lines") {
            val options = yamlFormatOptions {
                maximumEmptyLines = 0
                hasFinalLineBreak = false
            }

            formatted(lines("a: 1", "", "b: 2"), options) shouldBe lines("a: 1", "b: 2")
        }

        test("leaves blank lines inside block scalars alone") {
            val text = lines("a: |", "  x", "", "", "  y", "")

            formatted(text) shouldBe text
        }
    }

    context("ranges") {
        test("formats only the lines a range touches") {
            val text = lines(TRAILING_A, TRAILING_B)

            formatted(text, range = TextRange(start = 0, end = 1)) shouldBe lines("a: 1", TRAILING_B)
        }

        test("leaves the lines before a range alone") {
            val text = lines(TRAILING_A, TRAILING_B)

            formatted(
                text,
                range = TextRange(start = text.length - 1, end = text.length),
            ) shouldBe lines(TRAILING_A, "b: 2", "")
        }

        test("refuses a range whose edits would change the meaning") {
            val text = lines("a:", WIDE_B, "    c: 2")
            val tree = YamlSyntaxTree.parse(text)
            val result = YamlFormatter().format(tree, TextRange(start = 4, end = 5))

            result shouldBe YamlFormatResult.Refused(YamlFormatRefusal.MeaningChanged)
        }
    }

    context("guarantees") {
        test("refuses a document with syntax errors") {
            val result = YamlFormatter().format(YamlSyntaxTree.parse("a: [1"))

            result shouldBe YamlFormatResult.Refused(YamlFormatRefusal.SyntaxErrors)
        }

        test("rejects invalid options") {
            shouldThrow<ValidationException> { yamlFormatOptions { indent = 0 } }
            shouldThrow<ValidationException> { yamlFormatOptions { indent = TOO_WIDE } }
            shouldThrow<ValidationException> { yamlFormatOptions { maximumEmptyLines = -1 } }
        }

        test("describes its options") {
            YamlFormatOptions.Default.toString() shouldContain DEFAULT_INDENT
            YamlFormatter().toString() shouldContain DEFAULT_INDENT
        }

        test("refuses a range beyond the text") {
            val tree = YamlSyntaxTree.parse("a: 1")

            shouldThrow<IllegalArgumentException> {
                YamlFormatter().format(
                    tree,
                    TextRange(start = 0, end = tree.text.length + 1),
                )
            }
        }

        test("refuses a text that was too long to be read") {
            val tree = YamlSyntaxTree.parse("a: 1\nb: 2\n", yaml { limits { maximumInputLength = SMALL_INPUT } })

            YamlFormatter().format(tree) shouldBe YamlFormatResult.Refused(YamlFormatRefusal.SyntaxErrors)
        }

        test("formats a text whose formatted form is as long as the output limit") {
            val text = NARROW_DEEP
            val expected = WIDE_DEEP
            val atLimit = yaml { limits { maximumOutputLength = expected.length } }
            val result = YamlFormatter(yamlFormatOptions { indent = WIDE }).format(YamlSyntaxTree.parse(text, atLimit))

            result.shouldBeInstanceOf<YamlFormatResult.Formatted>().tree.text shouldBe expected
        }

        test("refuses a text whose formatted form would be one character longer than the output limit") {
            val text = NARROW_DEEP
            val expected = WIDE_DEEP
            val belowLimit = yaml { limits { maximumOutputLength = expected.length - 1 } }
            val wide = YamlFormatter(yamlFormatOptions { indent = WIDE })
            val result = wide.format(YamlSyntaxTree.parse(text, belowLimit))

            result shouldBe YamlFormatResult.Refused(YamlFormatRefusal.OutputTooLong)
        }

        test("refuses to widen a deep document past the output limit before it builds the text") {
            val text = buildString {
                repeat(DEEP_KEYS) { level -> append(" ".repeat(level)).append("k:\n") }
                repeat(DEEP_KEYS) { index -> append(" ".repeat(DEEP_KEYS) + "v$index: 1\n") }
            }
            val bounded = yaml { limits { maximumOutputLength = text.length * TRIPLE } }
            val options = yamlFormatOptions { indent = WIDEST }
            val widest = YamlFormatter(options)
            val tree = YamlSyntaxTree.parse(text, bounded)

            widest.format(tree) shouldBe YamlFormatResult.Refused(YamlFormatRefusal.OutputTooLong)
            LineLayout(tree, options).edits() shouldBe null
            widest.format(YamlSyntaxTree.parse(text)).shouldBeInstanceOf<YamlFormatResult.Formatted>()
        }

        test("leaves a formatted text alone") {
            val tree = YamlSyntaxTree.parse(lines("a:", NARROW_B, ""))
            val result = YamlFormatter().format(tree)

            result.shouldBeInstanceOf<YamlFormatResult.Formatted>().edits.shouldBeEmpty()
        }
    }

    context("layouts") {
        test("indent comments by the content line below them across several collections") {
            val text = lines(
                "a:",
                "      # one",
                "",
                "      # two",
                WIDE_B,
                "    c:",
                "            # three",
                "            d: 1",
            )

            formatted(
                text,
            ) shouldBe lines("a:", "  # one", "", "  # two", NARROW_B, "  c:", "    # three", "    d: 1", "")
        }

        test("keep a trailing comment at the end of the text where it is") {
            formatted(lines("a:", WIDE_B, "  # end")) shouldBe lines("a:", NARROW_B, "  # end", "")
        }

        test("leave whitespace inside multi-line flow scalars alone") {
            val text = lines("a: \"x  ", "  y\"", "b: 'p  ", "  q'", "")

            formatted(text) shouldBe text
        }

        test("indent nested sequences inside sequences") {
            formatted(lines("-   - a", "    - b", "-     c: 1", "      d: 2")) shouldBe
                lines("- - a", "  - b", "- c: 1", "  d: 2", "")
        }

        test("indent sequences of sequences when sequences stay at the column of their key") {
            val options = yamlFormatOptions { isSequenceIndented = false }

            formatted(lines("a:", "  - - x", "    - y"), options) shouldBe lines("a:", "- - x", "  - y", "")
        }
    }

    context("trees nested to the depth limit") {
        test("are laid out without exhausting the stack") {
            val levels = YamlLimits.HARD_DEPTH
            val tree = deepMappings(levels, indent = TRIPLE)
            val layout = LineLayout(tree, YamlFormatOptions.Default)
            val edits = layout.edits().shouldNotBeNull()
            val laidOut = deepMappings(levels, indent = 2)

            edits.size shouldBe levels
            tree.text.edited(edits) shouldBe laidOut.text + "\n"
        }
    }

    context("results") {
        val tree = YamlSyntaxTree.parse("a: 1")
        val edits = listOf(TextEdit(TextRange.at(tree.text.length), "\n"))

        test("compare by their content") {
            val formatted = YamlFormatResult.Formatted(edits, tree)
            val refused = YamlFormatResult.Refused(YamlFormatRefusal.MeaningChanged)

            formatted shouldBe YamlFormatResult.Formatted(edits, tree)
            formatted.hashCode() shouldBe YamlFormatResult.Formatted(edits, tree).hashCode()
            formatted shouldNotBe YamlFormatResult.Formatted(emptyList(), tree)
            formatted shouldNotBe YamlFormatResult.Formatted(edits, YamlSyntaxTree.parse("a: 1"))
            formatted.equals(refused) shouldBe false
            refused shouldBe YamlFormatResult.Refused(YamlFormatRefusal.MeaningChanged)
            refused.hashCode() shouldBe YamlFormatResult.Refused(YamlFormatRefusal.MeaningChanged).hashCode()
            refused shouldNotBe YamlFormatResult.Refused(YamlFormatRefusal.SyntaxErrors)
            refused.equals(formatted) shouldBe false
        }

        test("keep a copy of their edits") {
            val changing = edits.toMutableList()
            val formatted = YamlFormatResult.Formatted(changing, tree)
            changing.clear()

            formatted.edits shouldBe edits
        }

        test("describe themselves without the text they hold") {
            YamlFormatResult.Formatted(edits, tree).toString() shouldBe "Formatted(1 edits, $tree)"
            YamlFormatResult.Refused(YamlFormatRefusal.SyntaxErrors).toString() shouldBe "Refused(SyntaxErrors)"
        }
    }

    context("options") {
        test("compare by their values") {
            val wide = yamlFormatOptions { indent = WIDE }

            YamlFormatOptions.Default shouldBe yamlFormatOptions {}
            YamlFormatOptions.Default.hashCode() shouldBe yamlFormatOptions {}.hashCode()
            YamlFormatOptions.Default shouldNotBe wide
            YamlFormatOptions.Default.equals("indent=2") shouldBe false
            wide.indent shouldBe WIDE
        }

        test("cannot be changed once they are built") {
            withBuiltBuilder { builder ->
                shouldThrow<IllegalStateException> { builder.indent = WIDE }
                shouldThrow<IllegalStateException> { builder.isSequenceIndented = false }
                shouldThrow<IllegalStateException> { builder.maximumEmptyLines = 0 }
                shouldThrow<IllegalStateException> { builder.hasFinalLineBreak = false }
            }
        }
    }
})

private fun withBuiltBuilder(check: (YamlFormatOptionsBuilder) -> Unit) {
    var captured: YamlFormatOptionsBuilder? = null
    yamlFormatOptions { captured = this }
    check(requireNotNull(captured))
}
