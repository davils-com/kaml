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
package com.davils.kaml.core.node

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.fixture.timeRatio
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

private const val HEXADECIMAL: Int = 16
private const val OCTAL: Int = 8
private const val BINARY: Int = 2
private const val EIGHTY_ONES: String = "ffffffffffffffffffff"
private const val TWO_TO_THE_EIGHTY_LESS_ONE: String = "1208925819614629174706175"
private const val TWO_TO_THE_128_LESS_ONE: String = "340282366920938463463374607431768211455"
private const val TWO_TO_THE_64_LESS_ONE: String = "18446744073709551615"
private const val TWO_TO_THE_66_LESS_ONE: String = "73786976294838206463"
private const val LONG_HEX: Int = 32
private const val LONG_BINARY: Int = 64
private const val LONG_OCTAL: Int = 22
private const val TOTAL_DIGITS: Int = 256_000
private const val SHORT_DIGITS: Int = 125
private const val LONG_DIGITS: Int = 998
private const val MOST_GROWTH: Double = 4.0

private fun hexDocument(digits: Int): String = "- 0x${"f".repeat(digits)}\n".repeat(TOTAL_DIGITS / digits)

private fun textDocument(digits: Int): String = "- x${"f".repeat(digits + 1)}\n".repeat(TOTAL_DIGITS / digits)

private fun read(text: String) {
    Yaml.Default.parse(text)
}

class IntegerTextTest : FunSpec({
    context("a decimal") {
        test("drops leading zeros") {
            IntegerText.decimal(isNegative = false, digits = "0012") shouldBe "12"
            IntegerText.decimal(isNegative = true, digits = "0012") shouldBe "-12"
        }

        test("writes zero without a sign") {
            IntegerText.decimal(isNegative = true, digits = "000") shouldBe "0"
            IntegerText.decimal(isNegative = false, digits = "0") shouldBe "0"
        }
    }

    context("a long number of another radix") {
        test("is converted exactly in groups of digits") {
            IntegerText.ofRadix(isNegative = false, digits = "f".repeat(LONG_HEX), radix = HEXADECIMAL) shouldBe
                TWO_TO_THE_128_LESS_ONE
            IntegerText.ofRadix(isNegative = true, digits = "1".repeat(LONG_BINARY), radix = BINARY) shouldBe
                "-$TWO_TO_THE_64_LESS_ONE"
            IntegerText.ofRadix(isNegative = false, digits = "7".repeat(LONG_OCTAL), radix = OCTAL) shouldBe
                TWO_TO_THE_66_LESS_ONE
            IntegerText.ofRadix(isNegative = false, digits = "", radix = HEXADECIMAL) shouldBe "0"
        }

        test("is converted in time that hardly grows with its length") {
            val ratio = timeRatio(small = hexDocument(SHORT_DIGITS), large = hexDocument(LONG_DIGITS), read = ::read)
            ratio shouldBeLessThan MOST_GROWTH
        }

        test("costs little more to read than a string of the same length") {
            val ratio = timeRatio(small = textDocument(LONG_DIGITS), large = hexDocument(LONG_DIGITS), read = ::read)
            ratio shouldBeLessThan MOST_GROWTH
        }
    }

    context("a number of another radix") {
        test("is written in decimal") {
            IntegerText.ofRadix(isNegative = false, digits = "ff", radix = HEXADECIMAL) shouldBe "255"
            IntegerText.ofRadix(isNegative = false, digits = "FF", radix = HEXADECIMAL) shouldBe "255"
            IntegerText.ofRadix(isNegative = true, digits = "777", radix = OCTAL) shouldBe "-511"
            IntegerText.ofRadix(isNegative = false, digits = "1010", radix = BINARY) shouldBe "10"
        }

        test("keeps every digit of a number beyond every primitive type") {
            IntegerText.ofRadix(isNegative = false, digits = EIGHTY_ONES, radix = HEXADECIMAL) shouldBe
                TWO_TO_THE_EIGHTY_LESS_ONE
        }

        test("is zero without digits") {
            IntegerText.ofRadix(isNegative = true, digits = "", radix = HEXADECIMAL) shouldBe "0"
        }
    }

    context("a sexagesimal number") {
        test("counts every place as sixty of the next") {
            IntegerText.ofSexagesimal(isNegative = false, leading = "1", places = listOf("30")) shouldBe "90"
            IntegerText.ofSexagesimal(isNegative = true, leading = "190", places = listOf("20", "30")) shouldBe
                "-685230"
        }
    }
})
