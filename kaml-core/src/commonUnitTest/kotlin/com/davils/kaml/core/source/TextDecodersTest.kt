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
package com.davils.kaml.core.source

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf

private const val REPLACEMENT: String = "�"
private const val LETTER_A: Int = 0x61
private const val LETTER_B: Int = 0x62
private const val LETTER_C: Int = 0x63
private const val HIGH_SURROGATE: Int = 0xD800
private const val LOW_SURROGATE: Int = 0xDC00
private const val PRIVATE_USE: Int = 0xE000
private const val BEYOND_UNICODE: Int = 0x110000
private const val ALL_BITS: Int = -1
private const val PREFIX_BYTES: Int = 2
private const val UTF16_UNIT: Int = 2
private const val UTF32_UNIT: Int = 4
private const val SUPPLEMENTARY: String = "😀"
private const val LETTER_AND_SUPPLEMENTARY: String = "a$SUPPLEMENTARY"
private const val LETTER_AND_REPLACEMENT: String = "a$REPLACEMENT"

private val invalidUtf8: Map<String, ByteArray> = mapOf(
    "an overlong two byte form" to bytesOf(0xC0, 0x80),
    "an overlong three byte form" to bytesOf(0xE0, 0x80, 0x80),
    "an overlong four byte form" to bytesOf(0xF0, 0x80, 0x80, 0x80),
    "an encoded high surrogate" to bytesOf(0xED, 0xA0, 0x80),
    "an encoded low surrogate" to bytesOf(0xED, 0xB0, 0x80),
    "a value above U+10FFFF" to bytesOf(0xF4, 0x90, 0x80, 0x80),
    "a lead byte above F4" to bytesOf(0xF5, 0x80, 0x80, 0x80),
    "a sequence truncated by the end" to bytesOf(0xE2, 0x82),
    "a sequence truncated by an ASCII byte" to bytesOf(0xE2, 0x82, LETTER_A),
    "a stray continuation byte" to bytesOf(0x80),
)

private val invalidUtf16: Map<String, List<Int>> = mapOf(
    "a lone high surrogate at the end" to listOf(HIGH_SURROGATE),
    "a high surrogate before a letter" to listOf(HIGH_SURROGATE, LETTER_A),
    "a lone low surrogate" to listOf(LOW_SURROGATE, LETTER_A),
    "a reversed surrogate pair" to listOf(LOW_SURROGATE, HIGH_SURROGATE),
    "a high surrogate before a private use character" to listOf(HIGH_SURROGATE, PRIVATE_USE),
)

private val invalidUtf32: Map<String, List<Int>> = mapOf(
    "a value above U+10FFFF" to listOf(BEYOND_UNICODE),
    "a surrogate" to listOf(HIGH_SURROGATE),
    "a value with every bit set" to listOf(ALL_BITS),
)

class TextDecodersTest : FunSpec({
    context("UTF-8") {
        test("decodes sequences of one to four bytes") {
            val decoded = TextDecoders.decode("aä€$SUPPLEMENTARY".encodeToByteArray(), YamlEncoding.Utf8)

            decoded.text shouldBe "aä€$SUPPLEMENTARY"
            decoded.firstInvalidByte.shouldBeNull()
        }

        invalidUtf8.forEach { (name, bad) ->
            test(refusal(name)) {
                val bytes = bytesOf(LETTER_A, LETTER_B) + bad + bytesOf(LETTER_C)
                val decoded = TextDecoders.decode(bytes, YamlEncoding.Utf8)

                decoded.firstInvalidByte shouldBe PREFIX_BYTES
                decoded.text shouldStartWith "ab$REPLACEMENT"
                decoded.text shouldEndWith "c"
            }
        }

        test("names the first of several bad bytes") {
            val decoded = TextDecoders.decode(bytesOf(LETTER_A, 0x80, LETTER_B, 0xFF), YamlEncoding.Utf8)

            decoded.firstInvalidByte shouldBe 1
        }
    }

    listOf(true, false).forEach { isBigEndian ->
        val utf16 = YamlEncoding.Utf16BigEndian.takeIf { isBigEndian } ?: YamlEncoding.Utf16LittleEndian
        val utf32 = YamlEncoding.Utf32BigEndian.takeIf { isBigEndian } ?: YamlEncoding.Utf32LittleEndian

        context("$utf16") {
            test("decodes a surrogate pair") {
                val decoded = TextDecoders.decode(LETTER_AND_SUPPLEMENTARY.encodedAs(utf16), utf16)

                decoded.text shouldBe LETTER_AND_SUPPLEMENTARY
                decoded.firstInvalidByte.shouldBeNull()
            }

            invalidUtf16.forEach { (name, units) ->
                test(refusal(name)) {
                    val decoded = TextDecoders.decode(utf16Units(listOf(LETTER_A) + units, isBigEndian), utf16)

                    decoded.firstInvalidByte shouldBe UTF16_UNIT
                    decoded.text shouldStartWith LETTER_AND_REPLACEMENT
                }
            }

            test("refuses an odd number of bytes") {
                val bytes = utf16Units(listOf(LETTER_A), isBigEndian) + bytesOf(LETTER_B)
                val decoded = TextDecoders.decode(bytes, utf16)

                decoded.firstInvalidByte shouldBe UTF16_UNIT
                decoded.text shouldBe LETTER_AND_REPLACEMENT
            }
        }

        context("$utf32") {
            test("decodes a supplementary code point") {
                val decoded = TextDecoders.decode(LETTER_AND_SUPPLEMENTARY.encodedAs(utf32), utf32)

                decoded.text shouldBe LETTER_AND_SUPPLEMENTARY
                decoded.firstInvalidByte.shouldBeNull()
            }

            invalidUtf32.forEach { (name, units) ->
                test(refusal(name)) {
                    val decoded = TextDecoders.decode(utf32Units(listOf(LETTER_A) + units, isBigEndian), utf32)

                    decoded.firstInvalidByte shouldBe UTF32_UNIT
                    decoded.text shouldBe LETTER_AND_REPLACEMENT
                }
            }

            test("refuses a truncated unit") {
                val bytes = utf32Units(listOf(LETTER_A), isBigEndian) + bytesOf(0, 0)
                val decoded = TextDecoders.decode(bytes, utf32)

                decoded.firstInvalidByte shouldBe UTF32_UNIT
                decoded.text shouldBe LETTER_AND_REPLACEMENT
            }
        }
    }

    context("the diagnostic") {
        test("names the encoding and the offset of the first bad byte") {
            val bytes = "a: b".encodeToByteArray() + bytesOf(0xC3, 0x28)
            val failed = Yaml.Default.parse(YamlSource.of(bytes)).shouldBeInstanceOf<YamlParseResult.Failed>()
            val diagnostic = failed.diagnostics.single { it.code == YamlDiagnosticCode.InvalidEncoding }

            diagnostic.message shouldContain "Utf8"
            diagnostic.message shouldContain "byte 4"
        }
    }
})

private fun refusal(name: String): String = "refuses $name and names its first byte"
