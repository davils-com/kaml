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

package com.davils.kaml.document.patch.graph

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.internal.JsonEquality
import com.davils.kaml.document.internal.NodeCount
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.patch.JsonPatchOperation
import com.davils.kaml.document.patch.JsonPatchRefusal
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kaml.document.pointer.PointerSteps

internal class GraphOperations(limits: YamlLimits) {
    private val budget = WorkBudget(limits.maximumNodes, "maximumNodes")
    private val count = NodeCount(budget, limits.maximumDepth)
    private val equality = JsonEquality(budget, limits.maximumDepth)
    private val updates = GraphUpdates(budget)

    val work: WorkBudget get() = budget

    fun applied(operation: JsonPatchOperation, root: YamlNode): GraphStep = try {
        unchecked(operation, root)
    } catch (expected: YamlDocumentLimitException) {
        GraphStep.Refused(JsonPatchRefusal.LimitExceeded)
    }

    fun added(root: YamlNode, path: JsonPointer, value: YamlNode): GraphStep {
        count.charge(value, path.tokens.size)
        return placed(root, path, value)
    }

    fun removed(root: YamlNode, path: JsonPointer): GraphStep {
        val parent = path.parent ?: return GraphStep.Refused(JsonPatchRefusal.RootRemoval)
        return updates.at(root, parent) { node -> ParentChanges.removed(node, path.tokens.last()) }
    }

    private fun unchecked(operation: JsonPatchOperation, root: YamlNode): GraphStep = when (operation) {
        is JsonPatchOperation.Add -> added(root, operation.path, operation.value)
        is JsonPatchOperation.Remove -> removed(root, operation.path)
        is JsonPatchOperation.Replace -> replaced(root, operation.path, operation.value)
        is JsonPatchOperation.Move -> moved(root = root, from = operation.from, path = operation.path)
        is JsonPatchOperation.Copy -> copied(root = root, from = operation.from, path = operation.path)
        is JsonPatchOperation.Test -> tested(root, operation.path, operation.value)
    }

    private fun placed(root: YamlNode, path: JsonPointer, value: YamlNode): GraphStep {
        val parent = path.parent ?: return GraphStep.Done(value)
        return updates.at(root, parent) { node -> ParentChanges.added(node, path.tokens.last(), value) }
    }

    private fun replaced(root: YamlNode, path: JsonPointer, value: YamlNode): GraphStep {
        count.charge(value, path.tokens.size)
        val parent = path.parent ?: return GraphStep.Done(value)
        return updates.at(root, parent) { node -> ParentChanges.replaced(node, path.tokens.last(), value) }
    }

    private fun moved(root: YamlNode, from: JsonPointer, path: JsonPointer): GraphStep {
        val isIntoItself = path.startsWith(from) && path != from
        if (isIntoItself) return GraphStep.Refused(JsonPatchRefusal.MoveIntoItself)
        return valueAt(root, from).then { value ->
            count.checkDepth(path.tokens.size + value.depth)
            removed(root, from).then { without -> placed(without, path, value) }
        }
    }

    private fun copied(root: YamlNode, from: JsonPointer, path: JsonPointer): GraphStep =
        valueAt(root, from).then { value -> added(root, path, value) }

    private fun tested(root: YamlNode, path: JsonPointer, value: YamlNode): GraphStep =
        valueAt(root, path).then { actual ->
            val isEqual = equality.isEqual(left = actual, right = value)
            GraphStep.Done(root).takeIf { isEqual } ?: GraphStep.Refused(JsonPatchRefusal.TestFailed)
        }

    private fun valueAt(root: YamlNode, path: JsonPointer): GraphStep {
        val value = PointerSteps.resolved(root, path, budget)
            ?: return GraphStep.Refused(JsonPatchRefusal.MissingTarget)
        return GraphStep.Done(value)
    }
}
