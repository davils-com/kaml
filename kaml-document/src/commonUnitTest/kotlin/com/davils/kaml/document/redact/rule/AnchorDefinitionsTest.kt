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

package com.davils.kaml.document.redact.rule

import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.document.fixture.lines
import com.davils.kore.core.text.TextRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private val fallback: TextRange = TextRange(start = 0, end = 0)

class AnchorDefinitionsTest : FunSpec({
    context("anchors") {
        test("resolve to the latest definition before the alias and to nothing before the first or for a stranger") {
            val anchors = AnchorDefinitions(YamlSyntaxTree.parse(lines("a: &k x", "b: *k", "c: &k y", "d: *k")))

            anchors.rangeOf("k", aliasOffset = 11) shouldBe TextRange(start = 3, end = 7)
            anchors.rangeOf("k", aliasOffset = 25) shouldBe TextRange(start = 17, end = 21)
            anchors.rangeOf("k", aliasOffset = 0).shouldBeNull()
            anchors.rangeOf("q", aliasOffset = 11).shouldBeNull()
            anchors.rangeOf("q", aliasOffset = 11, default = fallback) shouldBe fallback
        }
    }
})
