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
package com.davils.kaml.core

import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.node.YamlDocument
import com.davils.kaml.core.node.YamlStream
import com.davils.kaml.core.node.yamlNull
import com.davils.kaml.core.node.yamlScalar
import com.davils.kore.core.text.TextRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private val stream: YamlStream = YamlStream(listOf(YamlDocument(yamlNull())))
private val otherStream: YamlStream = YamlStream(listOf(YamlDocument(yamlScalar("a"))))
private val warning: YamlDiagnostic =
    YamlDiagnostic(YamlDiagnosticCode.ReservedDirective, TextRange.at(0), "A reserved directive")
private val error: YamlDiagnostic = YamlDiagnostic(YamlDiagnosticCode.InvalidTag, TextRange.at(0), "A broken tag")

class YamlParseResultTest : FunSpec({
    context("a parsed result") {
        val parsed = YamlParseResult.Parsed(stream, listOf(warning))

        test("compares its stream and its diagnostics") {
            val anything: Any = parsed

            parsed shouldBe YamlParseResult.Parsed(stream, listOf(warning))
            parsed.hashCode() shouldBe YamlParseResult.Parsed(stream, listOf(warning)).hashCode()
            parsed shouldNotBe YamlParseResult.Parsed(otherStream, listOf(warning))
            parsed shouldNotBe YamlParseResult.Parsed(stream, emptyList())
            anything shouldNotBe YamlParseResult.Failed(listOf(warning), stream)
        }

        test("keeps a copy of its diagnostics") {
            val found = mutableListOf(warning)
            val result = YamlParseResult.Parsed(stream, found)
            found.clear()

            result.diagnostics shouldBe listOf(warning)
        }

        test("shows only the numbers of documents and diagnostics") {
            parsed.toString() shouldBe "Parsed(documents=1, diagnostics=1)"
        }
    }

    context("a failed result") {
        val failed = YamlParseResult.Failed(listOf(error), stream)

        test("compares its diagnostics and its partial stream") {
            val anything: Any = failed

            failed shouldBe YamlParseResult.Failed(listOf(error), stream)
            failed.hashCode() shouldBe YamlParseResult.Failed(listOf(error), stream).hashCode()
            failed shouldNotBe YamlParseResult.Failed(listOf(warning), stream)
            failed shouldNotBe YamlParseResult.Failed(listOf(error), otherStream)
            anything shouldNotBe YamlParseResult.Parsed(stream, listOf(error))
        }

        test("keeps a copy of its diagnostics") {
            val found = mutableListOf(error)
            val result = YamlParseResult.Failed(found, stream)
            found.clear()

            result.diagnostics shouldBe listOf(error)
        }

        test("shows only the numbers of diagnostics and documents") {
            failed.toString() shouldBe "Failed(diagnostics=1, documents=1)"
        }
    }
})
