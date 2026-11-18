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

package com.davils.kaml.core.syntax.ast

import com.davils.kaml.core.fixture.GROWTH_FACTOR
import com.davils.kaml.core.fixture.fastestRun
import com.davils.kaml.core.fixture.shouldGrowLinearlyFrom
import com.davils.kaml.core.syntax.YamlSyntaxTree
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration

private const val COMMENTS: Int = 10_000

private fun entryBelow(comments: Int): YamlEntrySyntax {
    val text = (0 until comments).joinToString(separator = "") { line -> "# $line\n" } + "a: 1\n"
    val document = YamlSyntaxTree.parse(text).documents.single()
    val mapping = document.root as YamlValueSyntax.Mapping
    return mapping.entries.single()
}

private fun leadingCommentsTime(comments: Int): Duration {
    val entry = entryBelow(comments)
    return fastestRun { entry.leadingComments.size shouldBe comments }
}

class CommentIndexTest : FunSpec({
    context("the comments above an entry") {
        test("are read in the order of the text") {
            entryBelow(comments = 3).leadingComments shouldBe listOf("0", "1", "2")
        }

        test("take time that grows linearly with their number") {
            val small = leadingCommentsTime(COMMENTS)
            val large = leadingCommentsTime(COMMENTS * GROWTH_FACTOR)

            large shouldGrowLinearlyFrom small
        }
    }
})
