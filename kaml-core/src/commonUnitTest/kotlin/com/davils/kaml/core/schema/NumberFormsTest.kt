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

import com.davils.kore.core.Limits
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val TOO_LARGE: String = "1e400"
private const val TOO_SMALL: String = "1e-400"
private const val ONE_AND_A_HALF: String = "1.5"

class NumberFormsTest : FunSpec({
    context("JSON numbers") {
        mapOf("-0.5e+10" to true, "1." to true, "0" to true, "01" to false, "-" to false, "1e" to false, "1E+" to false)
            .forEach { (text, expected) ->
                test("read \"$text\" as ${"a number".takeIf { expected } ?: "no number"}") {
                    NumberForms.isJsonNumber(text) shouldBe expected
                }
            }

        mapOf("-0" to true, "12" to true, "-01" to false, "" to false, "1.0" to false).forEach { (text, expected) ->
            test("read \"$text\" as ${"an integer".takeIf { expected } ?: "no integer"}") {
                NumberForms.isJsonInteger(text) shouldBe expected
            }
        }
    }

    context("core numbers") {
        test("accept a signed decimal") {
            NumberForms.isCoreDecimal("+12") shouldBe true
            NumberForms.isCoreDecimal("+") shouldBe false
            NumberForms.isCoreDecimal("1a") shouldBe false
        }

        test("read the special floats") {
            NumberForms.specialFloat("+.inf") shouldBe Double.POSITIVE_INFINITY
            NumberForms.specialFloat("-.Inf") shouldBe Double.NEGATIVE_INFINITY
            NumberForms.specialFloat(".NAN")?.isNaN() shouldBe true
            NumberForms.specialFloat("-.nan").shouldBeNull()
            NumberForms.specialFloat("inf").shouldBeNull()
        }

        test("need digits after a radix prefix") {
            NumberForms.isRadix(text = "0x1F", prefix = "0x", isDigit = TextScan::isHexDigit) shouldBe true
            NumberForms.isRadix(text = "0x", prefix = "0x", isDigit = TextScan::isHexDigit) shouldBe false
            NumberForms.isRadix(text = "0o8", prefix = "0o", isDigit = TextScan::isOctalDigit) shouldBe false
            NumberForms.isRadix(text = "x1", prefix = "0x", isDigit = TextScan::isHexDigit) shouldBe false
        }

        test("need a sign in an exponent when it is required") {
            NumberForms.exponent(TextScan("e3"), isSignRequired = true) shouldBe false
            NumberForms.exponent(TextScan("e+3"), isSignRequired = true) shouldBe true
            NumberForms.exponent(TextScan("3"), isSignRequired = true) shouldBe true
        }
    }

    context("conversion to a double") {
        test("overflows to an infinity with the sign of the text") {
            NumberForms.finite(TOO_LARGE) shouldBe Double.POSITIVE_INFINITY
            NumberForms.finite("-$TOO_LARGE") shouldBe Double.NEGATIVE_INFINITY
        }

        test("underflows to zero") {
            NumberForms.finite(TOO_SMALL) shouldBe 0.0
        }

        test("reads a leading plus") {
            NumberForms.finite("+$ONE_AND_A_HALF") shouldBe ONE_AND_A_HALF.toDouble()
        }

        test("reads a number as long as the decimal limit") {
            NumberForms.finite("1".padStart(Limits.MAX_DECIMAL_TEXT_LENGTH, '0')) shouldBe 1.0
        }

        test("refuses a number longer than the decimal limit") {
            NumberForms.finite("1".padStart(Limits.MAX_DECIMAL_TEXT_LENGTH + 1, '0')).shouldBeNull()
        }
    }
})
