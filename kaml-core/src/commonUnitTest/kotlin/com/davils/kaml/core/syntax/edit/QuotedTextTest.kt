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

package com.davils.kaml.core.syntax.edit

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.fixture.lines
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.syntax.edit.fixture.writableTexts
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.unsafeCodePointsIn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll

private const val CASES: Int = 100

private fun parsedKey(quoted: String): String {
    val parsed = Yaml.Default.parse("$quoted: 1").shouldBeInstanceOf<YamlParseResult.Parsed>()
    val document = parsed.stream.documents.single()
    val mapping = document.root.shouldBeInstanceOf<YamlNode.Mapping>()
    return mapping.keys
        .single()
        .shouldBeInstanceOf<YamlNode.Scalar>()
        .value
}

private fun quotedAs(escaped: String): String = "\"$escaped\""

class QuotedTextTest : FunSpec({
    context("quoting") {
        test("escapes what could end the quotes or break the line") {
            QuotedText.of(lines("a\"b\\c", "d\te\rf")) shouldBe quotedAs("""a\"b\\c\nd\te\rf""")
        }

        test("escapes hidden and invisible characters by their code point") {
            QuotedText.of(HostileFragments.ESCAPE) shouldBe quotedAs("""\x1B""")
            QuotedText.of(HostileFragments.SOFT_HYPHEN) shouldBe quotedAs("""\xAD""")
            QuotedText.of(HostileFragments.RIGHT_TO_LEFT_OVERRIDE) shouldBe quotedAs("""\u202E""")
            QuotedText.of(HostileFragments.TAG_LATIN_A) shouldBe quotedAs("""\U000E0041""")
            QuotedText.of(HostileFragments.LONE_HIGH_SURROGATE + "a") shouldBe quotedAs("""\uD800a""")
            QuotedText.of("a" + HostileFragments.LONE_HIGH_SURROGATE) shouldBe quotedAs("""a\uD800""")
        }

        test("keeps visible characters as they are") {
            QuotedText.of(
                "é \uD83D\uDE00 ${HostileFragments.HEBREW_ALEF}",
            ) shouldBe "\"é \uD83D\uDE00 ${HostileFragments.HEBREW_ALEF}\""
        }

        test("keeps letters that only newer Unicode tables know as they are, on every target") {
            val garay = "\uD803\uDD50"
            QuotedText.of(garay) shouldBe quotedAs(garay)
            QuotedText.isVisible(garay) shouldBe true
        }

        test("writes text that is safe to show and parses back to the same string") {
            checkAll(CASES, writableTexts) { text ->
                val quoted = QuotedText.of(text)

                unsafeCodePointsIn(quoted).shouldBeEmpty()
                parsedKey(quoted) shouldBe text
            }
        }
    }

    context("visibility") {
        test("tells text shown as it is from text that needs escapes") {
            QuotedText.isShownAsIs("plain key \uD83D\uDE00") shouldBe true
            QuotedText.isShownAsIs("tab\t") shouldBe false
            QuotedText.isShownAsIs(HostileFragments.RIGHT_TO_LEFT_OVERRIDE) shouldBe false
            QuotedText.isShownAsIs("quote\"") shouldBe false
            QuotedText.isVisible("quote\"") shouldBe true
            QuotedText.isVisible(HostileFragments.SOFT_HYPHEN) shouldBe false
        }
    }

    context("input checks") {
        test("refuse text YAML cannot represent and name its code point only") {
            val refused = shouldThrow<IllegalArgumentException> { EditInput.requireWritable("secret\uFFFF") }

            refused.message shouldBe "YAML cannot represent U+FFFF, so the editor cannot write it"
        }
    }
})
