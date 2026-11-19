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

package com.davils.kaml.serialization

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.serialization.fixture.Numbers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val HUGE_DIGITS: Int = 40
private const val OVERFLOW_DIGITS: Int = 400

private val format: YamlFormat = YamlFormat.Default

private fun codes(text: String): List<YamlDiagnosticCode> = format.decodeOrFailure<Numbers>(text).diagnostics.map {
    it.code
}

private fun numbers(text: String): Numbers = format.decodeFromString(Numbers.serializer(), text)

class YamlFormatNumbersTest : FunSpec({
    context("integers") {
        test("decode every Long from its canonical decimal") {
            numbers("long: -9223372036854775808") shouldBe Numbers(long = Long.MIN_VALUE)
            numbers("long: 9223372036854775807") shouldBe Numbers(long = Long.MAX_VALUE)
            numbers("long: 0x7FFFFFFFFFFFFFFF") shouldBe Numbers(long = Long.MAX_VALUE)
            numbers("long: -0") shouldBe Numbers(long = 0)
        }

        test("refuse a Long beyond its range") {
            codes("long: 9223372036854775808") shouldBe listOf(YamlDiagnosticCode.NumberOutOfRange)
            codes("long: -9223372036854775809") shouldBe listOf(YamlDiagnosticCode.NumberOutOfRange)
            format
                .decodeOrFailure<Numbers>("long: 9223372036854775808")
                .diagnostics
                .single()
                .message shouldBe
                "The integer is outside -9223372036854775808..9223372036854775807"
        }

        test("check Byte, Short and Int at both ends of their range") {
            numbers("{byte: -128, short: -32768, int: -2147483648}") shouldBe
                Numbers(byte = Byte.MIN_VALUE, short = Short.MIN_VALUE, int = Int.MIN_VALUE)
            numbers("{byte: 127, short: 32767, int: 2147483647}") shouldBe
                Numbers(byte = Byte.MAX_VALUE, short = Short.MAX_VALUE, int = Int.MAX_VALUE)
            listOf("byte: 128", "byte: -129", "short: 32768", "short: -32769", "int: 2147483648", "int: -2147483649")
                .forEach { line -> codes(line) shouldBe listOf(YamlDiagnosticCode.NumberOutOfRange) }
        }

        test("refuse an integer far beyond every range") {
            codes("int: ${"9".repeat(HUGE_DIGITS)}") shouldBe listOf(YamlDiagnosticCode.NumberOutOfRange)
        }

        test("refuse a float or text where an integer belongs") {
            codes("int: 1.0") shouldBe listOf(YamlDiagnosticCode.TypeMismatch)
            codes("long: text") shouldBe listOf(YamlDiagnosticCode.TypeMismatch)
        }
    }

    context("floats") {
        test("decode the largest finite Float") {
            numbers("float: 3.4028235e38") shouldBe Numbers(float = Float.MAX_VALUE)
        }

        test("refuse a Float beyond its range instead of turning it into infinity") {
            codes("float: 3.5e38") shouldBe listOf(YamlDiagnosticCode.NumberOutOfRange)
            codes("float: -3.5e38") shouldBe listOf(YamlDiagnosticCode.NumberOutOfRange)
            format
                .decodeOrFailure<Numbers>("float: 3.5e38")
                .diagnostics
                .single()
                .message shouldBe
                "The number is outside the range of a Float"
        }

        test("refuse a number beyond the range of a Double") {
            codes("double: 1e400") shouldBe listOf(YamlDiagnosticCode.InvalidTaggedValue)
            codes("float: -1e400") shouldBe listOf(YamlDiagnosticCode.InvalidTaggedValue)
            codes("double: 1${"0".repeat(OVERFLOW_DIGITS)}") shouldBe listOf(YamlDiagnosticCode.NumberOutOfRange)
        }

        test("accept infinity and not-a-number where the document writes them") {
            numbers("{float: .inf, double: -.Inf}") shouldBe
                Numbers(float = Float.POSITIVE_INFINITY, double = Double.NEGATIVE_INFINITY)
            numbers("{float: -.INF, double: +.inf}") shouldBe
                Numbers(float = Float.NEGATIVE_INFINITY, double = Double.POSITIVE_INFINITY)
            numbers("{float: .nan, double: .NaN}") shouldBe Numbers(float = Float.NaN, double = Double.NaN)
        }

        test("accept infinity from a node built in code") {
            val node = yamlMapping { "double" to yamlScalar(Double.POSITIVE_INFINITY) }

            format.decodeFromNode<Numbers>(node) shouldBe Numbers(double = Double.POSITIVE_INFINITY)
        }

        test("read an integer as a floating point number") {
            numbers("{float: 2, double: -7}") shouldBe Numbers(float = 2.0f, double = -7.0)
        }

        test("refuse text where a number belongs") {
            codes("double: text") shouldBe listOf(YamlDiagnosticCode.TypeMismatch)
            val failed = Yaml.Default.parse("double: !!float x").shouldBeInstanceOf<YamlParseResult.Failed>()
            val tagged = failed.partial.documents
                .single()
                .root

            format.decodeOrFailure<Numbers>(tagged).diagnostics.map { it.code } shouldBe
                listOf(YamlDiagnosticCode.TypeMismatch)
        }

        test("round trip the extremes") {
            val extremes = Numbers(
                byte = Byte.MIN_VALUE,
                short = Short.MAX_VALUE,
                int = Int.MIN_VALUE,
                long = Long.MIN_VALUE,
                float = Float.MIN_VALUE,
                double = -Double.MAX_VALUE,
            )

            numbers(format.encodeToString(Numbers.serializer(), extremes)) shouldBe extremes
        }
    }
})
