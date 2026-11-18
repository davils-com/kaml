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

package com.davils.kaml.core.conformance

import com.davils.kaml.core.YamlDuplicateKeys
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.conformance.fixture.JsonView
import com.davils.kaml.core.conformance.fixture.SuiteCases
import com.davils.kaml.core.yaml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class YamlCompositionTest : FunSpec({
    val yaml = yaml { duplicateKeys = YamlDuplicateKeys.KeepLast }

    context("the representation graph of the yaml-test-suite") {
        SuiteCases.all.filter { case -> !case.isError && case.json != null }.forEach { case ->
            test("${case.id} composes to its JSON") {
                val result = yaml.parse(case.yaml)
                val parsed = result.shouldBeInstanceOf<YamlParseResult.Parsed>()
                val actual = parsed.stream.documents.map { document -> JsonView.of(document.root) }
                val expected = JsonView.expected(case.json.orEmpty())

                if (actual.none { it == null }) actual shouldBe expected
            }
        }
    }

    context("emitting the representation graph of the yaml-test-suite") {
        SuiteCases.all.filter { case -> !case.isError }.forEach { case ->
            test("${case.id} reads back from its emitted text") {
                val parsed = yaml.parse(case.yaml) as? YamlParseResult.Parsed
                val stream = parsed?.stream
                if (stream != null) {
                    val emitted = yaml.emit(stream)
                    val reread = yaml.parse(emitted).shouldBeInstanceOf<YamlParseResult.Parsed>()

                    reread.stream.documents.map { it.root } shouldBe stream.documents.map { it.root }
                }
            }
        }
    }
})
