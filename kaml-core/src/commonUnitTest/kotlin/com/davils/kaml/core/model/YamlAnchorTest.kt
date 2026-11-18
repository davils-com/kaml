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
package com.davils.kaml.core.model

import com.davils.kaml.core.fixture.everyHostileFragment
import com.davils.kaml.core.fixture.hostileText
import com.davils.kaml.core.fixture.shouldBeSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.checkAll

private const val SECRET: String = "secret"
private const val LONGEST: Int = YamlAnchor.MAX_NAME_LENGTH

private val validNames: List<String> = listOf(
    "a",
    "id001",
    "😁",
    ":@*!$\"<foo>:",
    "a:",
    "café",
    "\u0301mark",
    "x".repeat(LONGEST),
)

private val invalidNames: List<String> = listOf(
    "",
    "x".repeat(LONGEST + 1),
    "a b",
    "a\tb",
    "a b",
    "a　b",
    "a\u2009b",
    "a\nb",
    "a\rb",
    "a\u2028b",
    "a\u2029b",
    "a,b",
    "a[b",
    "a]b",
    "a{b",
    "a}b",
    "a\u0000b",
    "a\u007Fb",
    "a\u0085b",
    "a\u009Bb",
    "a\u202Eb",
    "a\u200Eb",
    "a\u2066b",
    "a\u061Cb",
    "a\u00ADb",
    "a\uFEFFb",
    "a\u200Bb",
    "a\u3164b",
    "a\uFE0Fb",
    "a\uFFFEb",
    "a\uFFFFb",
    "a\uE000b",
    "a\uD800b",
    "a\uDC00b",
    "a\uD800",
    "\uDE01\uD83D",
)

class YamlAnchorTest : FunSpec({
    context("a valid name") {
        validNames.forEach { name ->
            test("is accepted with ${name.length} characters") {
                YamlAnchor.isValidName(name) shouldBe true
                YamlAnchor(name).name shouldBe name
            }
        }
    }

    context("an invalid name") {
        invalidNames.forEachIndexed { index, name ->
            test("is refused for case $index") {
                YamlAnchor.isValidName(name) shouldBe false
                shouldThrow<IllegalArgumentException> { YamlAnchor(name) }
            }
        }

        test("is refused without repeating the name") {
            val error = shouldThrow<IllegalArgumentException> { YamlAnchor("$SECRET\u202E") }

            error.message.orEmpty() shouldNotContain SECRET
        }

        test("is refused for hostile texts or shown safely when accepted") {
            checkAll(hostileText) { text ->
                val isValid = YamlAnchor.isValidName(text)
                val shown = YamlAnchor("a").takeIf { !isValid } ?: YamlAnchor(text)

                shown.toString().shouldBeSafeToShow()
            }
        }
    }

    context("the text form") {
        test("starts with an ampersand") {
            YamlAnchor("anchor").toString() shouldBe "&anchor"
        }

        test("escapes a leading combining mark") {
            val shown = YamlAnchor("\u0301mark").toString()

            shown shouldStartWith "&\\u{0301}"
            shown.shouldBeSafeToShow()
        }

        test("stays bounded for the longest name") {
            val shown = YamlAnchor("x".repeat(LONGEST)).toString()

            (shown.length < LONGEST) shouldBe true
        }

        test("is refused for every hostile fragment together") {
            YamlAnchor.isValidName(everyHostileFragment) shouldBe false
        }
    }
})
