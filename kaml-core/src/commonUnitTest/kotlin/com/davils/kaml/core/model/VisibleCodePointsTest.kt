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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val GARAY_LETTER: String = "\uD803\uDD50"
private const val TULU_TIGALARI_LETTER: String = "\uD804\uDF80"
private const val BEYOND_UNICODE: Int = 0x110000

private val hiddenCodePoints: Map<String, Int> = mapOf(
    "a control character" to 0x1B,
    "a C1 control character" to 0x85,
    "a soft hyphen" to 0xAD,
    "an Arabic number sign" to 0x600,
    "a zero width space" to 0x200B,
    "a right-to-left override" to 0x202E,
    "a right-to-left isolate" to 0x2067,
    "a line separator" to 0x2028,
    "a byte order mark" to 0xFEFF,
    "a Hangul filler" to 0x3164,
    "a variation selector" to 0xFE0F,
    "a lone surrogate" to 0xD800,
    "a private use character" to 0xE000,
    "a noncharacter" to 0xFFFF,
    "a tag character" to 0xE0041,
    "a supplementary variation selector" to 0xE0100,
    "a supplementary private use character" to 0x10FFFD,
)

private val visibleCodePoints: Map<String, Int> = mapOf(
    "a letter" to 'a'.code,
    "a letter of Latin-1" to 0xE9,
    "a letter of CJK" to 0x7000,
    "a letter of Garay, assigned in Unicode 16" to 0x10D50,
    "a letter of Tulu-Tigalari, assigned in Unicode 16" to 0x11380,
    "an emoji" to 0x1F600,
)

class VisibleCodePointsTest : FunSpec({
    context("a code point") {
        hiddenCodePoints.forEach { (name, codePoint) ->
            test("is hidden when it is $name") {
                VisibleCodePoints.isVisible(codePoint) shouldBe false
            }
        }

        visibleCodePoints.forEach { (name, codePoint) ->
            test("is visible when it is $name") {
                VisibleCodePoints.isVisible(codePoint) shouldBe true
            }
        }

        test("is hidden outside the range of Unicode") {
            VisibleCodePoints.isVisible(-1) shouldBe false
            VisibleCodePoints.isVisible(BEYOND_UNICODE) shouldBe false
        }
    }

    context("an anchor") {
        test("accepts letters that only newer Unicode tables know, on every target") {
            YamlAnchor.isValidName("a$GARAY_LETTER") shouldBe true
            YamlAnchor.isValidName("a$TULU_TIGALARI_LETTER") shouldBe true
        }

        test("refuses a hidden character, on every target") {
            YamlAnchor.isValidName("a\u202Eb") shouldBe false
            YamlAnchor.isValidName("a\u200Bb") shouldBe false
        }
    }
})
