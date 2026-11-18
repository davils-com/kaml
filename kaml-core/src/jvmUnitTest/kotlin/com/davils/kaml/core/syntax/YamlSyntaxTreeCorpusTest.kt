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

package com.davils.kaml.core.syntax

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.conformance.fixture.Corpus
import com.davils.kaml.core.event.YamlEvent
import com.davils.kaml.core.source.YamlSource
import com.davils.kaml.core.syntax.ast.YamlEntrySyntax
import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

private const val CASES: Int = 20
private const val EDIT_SPAN: Int = 3

private fun scalarsOf(value: YamlValueSyntax): List<String> = when (value) {
    is YamlValueSyntax.Scalar -> listOf(value.value)
    is YamlValueSyntax.Empty -> listOf("")
    is YamlValueSyntax.Alias -> emptyList()
    is YamlValueSyntax.Sequence -> value.items.flatMap(::scalarsOf)
    is YamlValueSyntax.Mapping -> value.entries.flatMap(::entryScalars)
}

private fun entryScalars(entry: YamlEntrySyntax): List<String> {
    val values = entry.value?.let(::scalarsOf) ?: listOf("")
    return scalarsOf(entry.key) + values
}

private fun documentScalars(tree: YamlSyntaxTree): List<String> =
    tree.documents.flatMap { document -> scalarsOf(document.root) }

class YamlSyntaxTreeCorpusTest : FunSpec({
    context("the yaml-test-suite") {
        val valid = Corpus.documents.filter { (id, _) -> id !in Corpus.errorIds }
        valid.forEach { (id, text) ->
            test("$id reads the same scalars as the event stream") {
                val tree = YamlSyntaxTree.parse(text)
                val reading = Yaml.Default.events(YamlSource.of(text))
                val expected = reading.events.filterIsInstance<YamlEvent.Scalar>().map { it.value }

                tree.text shouldBe text
                documentScalars(tree) shouldBe expected
            }
        }
    }

    context("incremental reparsing") {
        val texts = Corpus.documents.map { it.second }.filter { it.length > 1 }

        test("produces the tree of a full parse") {
            checkAll(
                iterations = CASES * CASES * CASES,
                genA = Arb.element(texts),
                genB = Arb.int(0..Int.MAX_VALUE),
                genC = Arb.element("a", "\n", ": ", "- x\n", "---\n", "#c", ""),
            ) { text, seed, insert ->
                val start = seed % text.length
                val end = minOf(a = text.length, b = start + seed % EDIT_SPAN)
                val edit = TextEdit(TextRange(start = start, end = end), insert)
                val incremental = YamlSyntaxTree.parse(text).edit(listOf(edit))
                val full = YamlSyntaxTree.parse(incremental.text)

                incremental.root.text shouldBe full.text
                incremental.diagnostics.map { it.code to it.range } shouldBe
                    full.diagnostics.map { it.code to it.range }
                incremental.documents.size shouldBe full.documents.size
            }
        }
    }
})
