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

package com.davils.kaml.core.syntax.format

import com.davils.kaml.core.conformance.fixture.Corpus
import com.davils.kaml.core.syntax.YamlSyntaxTree
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf

class YamlFormatterCorpusTest : FunSpec({
    context("formatting the yaml-test-suite") {
        test("keeps the meaning of every valid document of the corpus and is idempotent") {
            Corpus.documents.filter { (id, _) -> id !in Corpus.errorIds }.forEach { (id, text) ->
                withClue(id) {
                    val first = YamlFormatter().format(YamlSyntaxTree.parse(text))
                    val formatted = first.shouldBeInstanceOf<YamlFormatResult.Formatted>()
                    val second = YamlFormatter().format(formatted.tree)

                    second.shouldBeInstanceOf<YamlFormatResult.Formatted>().edits.shouldBeEmpty()
                }
            }
        }
    }
})
