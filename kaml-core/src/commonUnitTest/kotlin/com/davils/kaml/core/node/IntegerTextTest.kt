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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val HEXADECIMAL: Int = 16
private const val OCTAL: Int = 8
private const val BINARY: Int = 2
private const val EIGHTY_ONES: String = "ffffffffffffffffffff"
private const val TWO_TO_THE_EIGHTY_LESS_ONE: String = "1208925819614629174706175"

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
