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

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.patch.JsonPatchOperation
import com.davils.kaml.document.patch.JsonPatchRefusal
import com.davils.kaml.document.patch.graph.GraphOperations
import com.davils.kaml.document.patch.graph.GraphStep
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kaml.document.pointer.PointerSteps

internal class SyntaxOperations(
    private val state: SyntaxState,
    private val document: Int,
    private val graph: GraphOperations,
) {
    fun applied(operation: JsonPatchOperation): Outcome = when (val step = graph.applied(operation, state.root)) {
        is GraphStep.Refused -> Outcome.Refused(step.reason)
        is GraphStep.Done -> outcomeOf(edited(operation, step.node), step.node)
    }

    private fun outcomeOf(step: SyntaxStep, root: YamlNode): Outcome = when (step) {
        is SyntaxStep.Done -> Outcome.Done(SyntaxState(step.tree, root))
        is SyntaxStep.Refused -> Outcome.Refused(step.reason)
    }

    private fun edited(operation: JsonPatchOperation, result: YamlNode): SyntaxStep {
        val edits = SyntaxEdits(tree = state.tree, document = document, root = state.root, budget = graph.work)
        return when (operation) {
            is JsonPatchOperation.Add -> edits.added(operation.path, operation.value)
            is JsonPatchOperation.Remove -> edits.removed(operation.path)
            is JsonPatchOperation.Replace -> edits.replaced(operation.path, operation.value)
            is JsonPatchOperation.Move -> moved(edits, operation, result)
            is JsonPatchOperation.Copy -> copied(edits, operation)
            is JsonPatchOperation.Test -> SyntaxStep.Done(state.tree)
        }
    }

    private fun moved(edits: SyntaxEdits, operation: JsonPatchOperation.Move, result: YamlNode): SyntaxStep {
        val value = valueAt(operation.from)
        return edits.removed(operation.from).then { tree ->
            val placing = SyntaxEdits(tree = tree, document = document, root = result, budget = graph.work)
            placing.added(operation.path, value)
        }
    }

    private fun copied(edits: SyntaxEdits, operation: JsonPatchOperation.Copy): SyntaxStep =
        edits.added(operation.path, valueAt(operation.from))

    private fun valueAt(pointer: JsonPointer): YamlNode =
        checkNotNull(PointerSteps.resolved(state.root, pointer, graph.work))

    sealed interface Outcome {
        class Done(val state: SyntaxState) : Outcome

        class Refused(val reason: JsonPatchRefusal) : Outcome
    }
}
