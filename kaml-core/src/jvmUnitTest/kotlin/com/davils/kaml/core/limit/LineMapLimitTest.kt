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

package com.davils.kaml.core.limit

import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.source.YamlSource
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.yaml
import com.davils.kore.core.Limits
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private val beyondLineMap: String = "#" + "a".repeat(Limits.MAX_LINE_MAP_LENGTH)

class LineMapLimitTest : FunSpec({
    context("a source longer than a line map indexes") {
        val relaxed = yaml { limits = YamlLimits.Relaxed }

        test("is refused by parse under the relaxed limits") {
            val result = relaxed.parse(beyondLineMap)
            result.shouldBeInstanceOf<YamlParseResult.Failed>()
            result.diagnostics.map { it.code } shouldContainExactly listOf(YamlDiagnosticCode.InputTooLong)
        }

        test("is refused by events under the relaxed limits") {
            val codes = relaxed.events(YamlSource.of(beyondLineMap)).diagnostics.map { it.code }
            codes shouldContainExactly listOf(YamlDiagnosticCode.InputTooLong)
        }

        test("gives a syntax tree without documents and lines") {
            val tree = YamlSyntaxTree.parse(beyondLineMap, relaxed)
            tree.documents.shouldBeEmpty()
            tree.lines.lineCount shouldBe 1
        }
    }
})
