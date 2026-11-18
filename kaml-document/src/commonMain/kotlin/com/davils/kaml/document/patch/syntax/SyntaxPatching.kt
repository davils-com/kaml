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

package com.davils.kaml.document.patch.syntax

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.document.patch.JsonPatchEdit
import com.davils.kaml.document.patch.JsonPatchOperation
import com.davils.kaml.document.patch.graph.GraphOperations
import com.davils.kaml.document.patch.graph.GraphStep

internal class SyntaxPatching(
    private val tree: YamlSyntaxTree,
    private val document: Int,
    private val limits: YamlLimits,
) {
    fun apply(operations: List<JsonPatchOperation>): JsonPatchEdit = when (val root = SyntaxRoots.of(tree, document)) {
        is GraphStep.Refused -> JsonPatchEdit.Failed(0, root.reason)
        is GraphStep.Done -> applied(operations, SyntaxState(tree, root.node))
    }

    private fun applied(operations: List<JsonPatchOperation>, initial: SyntaxState): JsonPatchEdit {
        val graph = GraphOperations(limits)
        var state = initial
        var failure: JsonPatchEdit.Failed? = null
        var index = 0
        while (failure == null && index < operations.size) {
            when (val step = SyntaxOperations(state, document, graph).applied(operations[index])) {
                is SyntaxOperations.Outcome.Done -> state = step.state
                is SyntaxOperations.Outcome.Refused -> failure = JsonPatchEdit.Failed(index, step.reason)
            }
            index++
        }
        val last = (operations.size - 1).coerceAtLeast(0)
        return failure ?: SyntaxVerification(tree, document).finished(state, last, limits)
    }
}
