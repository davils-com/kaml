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

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.yaml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

private const val LIMIT: Int = 10
private const val BYTES: Int = 4_000
private const val MOST_DECODED: Int = LIMIT + 2

private val document: String = "a".repeat(BYTES / Char.SIZE_BYTES)

class DecodingLimitTest : FunSpec({
    context("decoding bytes") {
        YamlEncoding.entries.forEach { encoding ->
            test("in $encoding stops once the text passes the limit") {
                val bytes = document.encodedAs(encoding)
                val decoded = TextDecoders.decode(bytes, encoding, maximumLength = LIMIT)
                decoded.text.length shouldBeLessThanOrEqual MOST_DECODED
            }

            test("in $encoding reads the whole text within the limit") {
                val decoded = TextDecoders.decode(document.encodedAs(encoding), encoding, maximumLength = BYTES)
                decoded.text shouldBe document
            }
        }
    }

    context("a source of bytes longer than the input limit") {
        test("is refused after decoding no more than the limit") {
            val tight = yaml { limits { maximumInputLength = LIMIT } }
            val source = YamlSource.of(ByteArray(LIMIT * 2) { 'a'.code.toByte() })
            val codes = tight.events(source).diagnostics.map { it.code }
            codes shouldContainExactly listOf(YamlDiagnosticCode.InputTooLong)
        }
    }
})
