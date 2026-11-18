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
package com.davils.kaml.core.schema

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val DIGITS: String = "123"
private const val TWO: Int = 2

class TextScanTest : FunSpec({
    context("accepting") {
        test("takes one expected character") {
            val scan = TextScan("ab")

            scan.accept('b') shouldBe false
            scan.accept('a') shouldBe true
            scan.acceptAny("xy") shouldBe false
            scan.acceptAny("b") shouldBe true
            scan.acceptAny("b") shouldBe false
            scan.isAtEnd shouldBe true
        }

        test("takes a whole word or nothing") {
            val scan = TextScan("0x1")

            scan.acceptWord("0b") shouldBe false
            scan.acceptWord("0x") shouldBe true
            scan.sign() shouldBe false
        }
    }

    context("counting") {
        test("counts the accepted characters") {
            TextScan(DIGITS).count(TextScan::isDigit) shouldBe DIGITS.length
        }

        test("takes at most the asked number of characters") {
            TextScan(DIGITS).take(TWO, TextScan::isDigit) shouldBe "12"
        }

        test("checks a count between two bounds") {
            TextScan("1x").countBetween(least = TWO, most = TWO, isAccepted = TextScan::isDigit) shouldBe false
            TextScan(DIGITS).countBetween(least = 1, most = TWO, isAccepted = TextScan::isDigit) shouldBe true
        }
    }

    context("character classes") {
        test("recognise their characters") {
            TextScan.isDigitOrUnderscore('_') shouldBe true
            TextScan.isHexDigit('F') shouldBe true
            TextScan.isHexDigit('g') shouldBe false
            TextScan.isOctalDigit('8') shouldBe false
            TextScan.isBinaryDigit('1') shouldBe true
            TextScan.isBinaryDigit('2') shouldBe false
            TextScan.isBlank('\t') shouldBe true
            TextScan.isBlank('x') shouldBe false
        }
    }
})
