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
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val DOCUMENT: String = "port: eighty"
private const val FILE_NAME: String = "port.yaml"
private const val LETTER: Byte = 0x61
private const val MARK: String = "\uFEFF"
private val invalidUtf8: ByteArray = byteArrayOf(0x61, 0x3A, 0x20, 0xC3.toByte(), 0x28)

private val detections: Map<ByteArray, YamlEncoding> = mapOf(
    bytesOf(0, 0, 0xFE, 0xFF) to YamlEncoding.Utf32BigEndian,
    bytesOf(0, 0x61, 0, 0) to YamlEncoding.Utf32LittleEndian,
    bytesOf(0xFF, 0x61, 0, 0) to YamlEncoding.Utf32LittleEndian,
    bytesOf(0xFF, 0xFE, 0x61, 0) to YamlEncoding.Utf16LittleEndian,
    bytesOf(0xFF, 0xFE, 0, 0x61) to YamlEncoding.Utf16LittleEndian,
    bytesOf(0xFE, 0xFF) to YamlEncoding.Utf16BigEndian,
    bytesOf(0xFE, 0x61) to YamlEncoding.Utf8,
    bytesOf(0xFF, 0x61) to YamlEncoding.Utf8,
    bytesOf(0, 0, 0, 0) to YamlEncoding.Utf8,
)

class YamlSourceTest : FunSpec({
    context("encodings") {
        YamlEncoding.entries.forEach { encoding ->
            test("detects $encoding without a byte order mark") {
                SourceDecoding.encodingOf(DOCUMENT.encodedAs(encoding)) shouldBe encoding
            }

            test("detects $encoding with a byte order mark") {
                SourceDecoding.encodingOf((MARK + DOCUMENT).encodedAs(encoding)) shouldBe encoding
            }

            test("reads a document in $encoding") {
                val source = YamlSource.of((MARK + DOCUMENT).encodedAs(encoding), name = FILE_NAME)
                val result = Yaml.Default.parse(source).shouldBeInstanceOf<YamlParseResult.Parsed>()

                result.stream.single
                    ?.root["port"]
                    .string() shouldBe "eighty"
            }
        }

        test("falls back to UTF-8 for a single byte") {
            SourceDecoding.encodingOf(byteArrayOf(LETTER)) shouldBe YamlEncoding.Utf8
        }

        detections.forEach { (bytes, encoding) ->
            test("detects $encoding from ${bytes.contentToString()}") {
                SourceDecoding.encodingOf(bytes) shouldBe encoding
            }
        }

        test("falls back to UTF-8 for no bytes") {
            SourceDecoding.encodingOf(ByteArray(0)) shouldBe YamlEncoding.Utf8
        }
    }

    context("invalid input") {
        test("reports bytes that are no valid UTF-8") {
            val result = Yaml.Default.parse(YamlSource.of(invalidUtf8))

            result.diagnostics.map { it.code } shouldContain YamlDiagnosticCode.InvalidEncoding
        }
    }

    context("naming") {
        test("names a source after the name it was given") {
            YamlSource.of(DOCUMENT, name = FILE_NAME).toString() shouldContain FILE_NAME
        }

        test("calls a source without a name unnamed") {
            YamlSource.of(DOCUMENT).toString() shouldContain "unnamed"
        }
    }
})
