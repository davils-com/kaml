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

private fun sample(value: Double, text: String): Pair<Double, String> = value to text

private val samples: List<Pair<Double, String>> = listOf(
    sample(value = 0.1, text = "0.1"),
    sample(value = 1e21, text = "1.0E21"),
    sample(value = 1e-7, text = "1.0E-7"),
    sample(value = Double.MIN_VALUE, text = "5.0E-324"),
    sample(value = Double.MIN_VALUE * 2, text = "1.0E-323"),
    sample(value = Double.MIN_VALUE + Double.MIN_VALUE + Double.MIN_VALUE, text = "1.5E-323"),
    sample(value = -0.0, text = "-0.0"),
    sample(value = 0.0, text = "0.0"),
    sample(value = 1.0E10, text = "1.0E10"),
    sample(value = 123_456.789, text = "123456.789"),
    sample(value = 1.0, text = "1.0"),
    sample(value = 1.5, text = "1.5"),
    sample(value = -2.5, text = "-2.5"),
    sample(value = 100.0, text = "100.0"),
    sample(value = 1e7, text = "1.0E7"),
    sample(value = 9_999_999.0, text = "9999999.0"),
    sample(value = 0.001, text = "0.001"),
    sample(value = 1e-4, text = "1.0E-4"),
    sample(value = 1e23, text = "1.0E23"),
    sample(value = Double.MAX_VALUE, text = "1.7976931348623157E308"),
    sample(value = -Double.MIN_VALUE, text = "-5.0E-324"),
)

class FloatTextTest : FunSpec({
    context("a finite double") {
        samples.forEach { (value, text) ->
            test("is written as $text on every target") {
                FloatText.of(value) shouldBe text
            }

            test("reads back from $text as the same value") {
                NumberTexts.finiteOrNull(text) shouldBe value
            }
        }
    }

    context("a double that is not finite") {
        test("is written as YAML writes it") {
            FloatText.of(Double.NaN) shouldBe ".nan"
            FloatText.of(Double.POSITIVE_INFINITY) shouldBe ".inf"
            FloatText.of(Double.NEGATIVE_INFINITY) shouldBe "-.inf"
        }
    }
})
