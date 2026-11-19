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
package com.davils.kaml.core.parser

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.fixture.problems
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.model.YamlTag
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val HANDLE: String = "%TAG !e! tag:example.com,2026:"

private val invalidTags: Map<String, String> = mapOf(
    "an empty verbatim tag" to "!<> 1",
    "a verbatim tag of the non-specific tag" to "!<!> 1",
    "a verbatim tag that is no URI" to "!<$:?> 1",
    "a verbatim tag without a scheme" to "!<:x> 1",
    "the secondary handle without a suffix" to "!! 1",
    "a named handle without a suffix" to text(HANDLE, "--- !e! 1"),
    "a tag that decodes to a line feed" to "!a%0A 1",
    "a tag that decodes to a right-to-left override" to "!a%E2%80%AE 1",
    "a tag that decodes to a zero width space" to "!a%E2%80%8Bb 1",
    "a tag that decodes to a byte order mark" to "!a%EF%BB%BF 1",
)

private val validTags: Map<String, YamlTag> = mapOf(
    "!<tag:yaml.org,2002:str> a" to YamlTag.STRING,
    "!<!bar> a" to YamlTag("!bar"),
    text(HANDLE, "--- !e!tag%21 a") to YamlTag("tag:example.com,2026:tag!"),
    "!a%20b a" to YamlTag("!a b"),
    "!a%C3%A4 a" to YamlTag("!a\u00E4"),
)

class TagValidationTest : FunSpec({
    context("a tag") {
        invalidTags.forEach { (name, document) ->
            test("is refused when it is $name") {
                Yaml.Default.problems(document) shouldContain YamlDiagnosticCode.InvalidTag
            }
        }

        validTags.forEach { (document, tag) ->
            test("is read as ${tag.uri}") {
                val result = Yaml.Default.parse(document)
                result.shouldBeInstanceOf<YamlParseResult.Parsed>()
                val documents = result.stream.documents
                documents.single().root.tag shouldBe tag
            }
        }
    }

    context("a tag directive") {
        test("is refused when its prefix holds a hidden character") {
            val document = text("%TAG !e! tag:example.com,2026:\u202E", "--- !e!x 1")
            Yaml.Default.problems(document) shouldContain YamlDiagnosticCode.InvalidDirective
        }
    }
})
