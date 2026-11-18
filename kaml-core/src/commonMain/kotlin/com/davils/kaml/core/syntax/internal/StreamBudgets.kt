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

package com.davils.kaml.core.syntax.internal

import com.davils.kaml.core.compose.EventBudget
import com.davils.kaml.core.compose.EventWalker
import com.davils.kaml.core.compose.Located
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.syntax.YamlSyntaxKind

internal class StreamBudgets(private val limits: YamlLimits) {
    fun admits(root: GreenNode, text: String): Boolean {
        val documents = root.children.count { it.kind == YamlSyntaxKind.DOCUMENT }
        val mostNodes = NODES_PER_CHARACTER * text.length + documents + 1L
        val isSurelyWithin = documents <= limits.maximumDocuments && mostNodes <= limits.maximumNodes
        return isSurelyWithin || isWalkedWithin(root, text)
    }

    private fun isWalkedWithin(root: GreenNode, text: String): Boolean {
        val budget = EventBudget(limits)
        val walker = EventWalker(text = text, maximumDiagnostics = limits.maximumDiagnostics, sink = budget::admits)
        walker.stream(Located(root, 0))
        return !budget.isExhausted
    }

    private companion object {
        const val NODES_PER_CHARACTER: Long = 3L
    }
}
