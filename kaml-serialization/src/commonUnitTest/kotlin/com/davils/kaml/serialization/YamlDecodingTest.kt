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

import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.serialization.fixture.Listener
import com.davils.kore.core.Limits
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import kotlinx.serialization.decodeFromString

private const val KEYS: Int = 40
private const val VALUE: String = "value"
private const val LINE_FEED: Int = 10

private val warning: YamlDiagnostic = YamlDiagnostic(
    code = YamlDiagnosticCode.NewerMinorVersion,
    range = TextRange.at(0),
    message = "newer",
)

class YamlDecodingTest : FunSpec({
    context("a decoded value") {
        val decoded = YamlDecoding.Decoded(VALUE, listOf(warning))

        test("equals another with the same value and diagnostics") {
            decoded shouldBe YamlDecoding.Decoded(VALUE, listOf(warning))
            decoded.hashCode() shouldBe YamlDecoding.Decoded(VALUE, listOf(warning)).hashCode()
        }

        test("differs by value, by diagnostics and from a failure") {
            decoded shouldNotBe YamlDecoding.Decoded("other", listOf(warning))
            decoded shouldNotBe YamlDecoding.Decoded(VALUE, emptyList())
            decoded shouldNotBe YamlDecoding.Failed(listOf(warning))
        }

        test("hashes a null value") {
            val nothing = YamlDecoding.Decoded(null, emptyList())

            nothing.hashCode() shouldBe YamlDecoding.Decoded(null, emptyList()).hashCode()
        }

        test("shows only the number of its diagnostics") {
            decoded.toString() shouldBe "Decoded(diagnostics=1)"
        }

        test("keeps its diagnostics from being changed") {
            shouldThrow<UnsupportedOperationException> {
                (decoded.diagnostics as MutableList<YamlDiagnostic>).add(warning)
            }
        }
    }

    context("a failed decoding") {
        val failed = YamlDecoding.Failed(listOf(warning))

        test("equals another with the same diagnostics") {
            failed shouldBe YamlDecoding.Failed(listOf(warning))
            failed.hashCode() shouldBe YamlDecoding.Failed(listOf(warning)).hashCode()
            failed shouldNotBe YamlDecoding.Failed(emptyList())
            failed shouldNotBe YamlDecoding.Decoded(VALUE, listOf(warning))
        }

        test("shows only the number of its diagnostics") {
            failed.toString() shouldBe "Failed(diagnostics=1)"
        }
    }

    context("the exception") {
        test("bounds its message and names the problems it leaves out") {
            val keys = (1..KEYS).map { index -> YamlEntry(key = yamlScalar("extra$index"), value = yamlScalar(index)) }
            val node = yamlMapping(listOf(YamlEntry(key = yamlScalar("port"), value = yamlScalar(1))) + keys)
            val failure = shouldThrow<YamlDecodingException> { YamlFormat.Default.decodeFromNode<Listener>(node) }
            val message = failure.message.orEmpty()

            val lines = message.lines()

            lines.first() shouldBe "The YAML could not be decoded:"
            lines[1] shouldBe """1:1 There is no property "extra1" [kaml.decode.unknown-property]"""
            message shouldEndWith "more problems"
            message.length shouldBeLessThanOrEqual Limits.MAX_MESSAGE_LENGTH + "\nand 40 more problems".length
            failure.diagnostics.size shouldBe KEYS
        }

        test("keeps its diagnostics from being changed") {
            val failure = shouldThrow<YamlDecodingException> { YamlFormat.Default.decodeFromString<Listener>("x") }

            shouldThrow<UnsupportedOperationException> {
                (failure.diagnostics as MutableList<YamlDiagnostic>).clear()
            }
        }
    }

    context("the configuration") {
        test("describes itself without its serializers") {
            val discriminator = "kind" + Char(LINE_FEED)

            yamlFormat { classDiscriminator = discriminator }.toString() shouldBe
                "YamlFormat(YamlFormatConfiguration(isUnknownKeyIgnored=false, isDefaultEncoded=true, " +
                "hasExplicitNulls=true, isInputCoerced=false, isValueQuoted=false, polymorphism=Property, " +
                """classDiscriminator="kind\n"))"""
        }

        test("refuses an empty discriminator") {
            shouldThrow<ValidationException> { yamlFormat { classDiscriminator = "" } }
        }

        test("keeps the default discriminator") {
            YamlFormat.Default.configuration.classDiscriminator shouldBe YamlFormatBuilder.DEFAULT_DISCRIMINATOR
        }
    }
})
