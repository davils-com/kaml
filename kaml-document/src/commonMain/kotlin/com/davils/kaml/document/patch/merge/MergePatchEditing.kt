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

package com.davils.kaml.document.patch.merge

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.isNull
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.internal.NodeCount
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.patch.JsonPatchEdit
import com.davils.kaml.document.patch.JsonPatchRefusal
import com.davils.kaml.document.patch.graph.GraphStep
import com.davils.kaml.document.patch.syntax.SyntaxEdits
import com.davils.kaml.document.patch.syntax.SyntaxRoots
import com.davils.kaml.document.patch.syntax.SyntaxState
import com.davils.kaml.document.patch.syntax.SyntaxStep
import com.davils.kaml.document.patch.syntax.SyntaxVerification
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kaml.document.pointer.at

internal class MergePatchEditing(
    private val tree: YamlSyntaxTree,
    private val document: Int,
    private val limits: YamlLimits,
) {
    private val expectation = MergePatching(limits)
    private val values = MergePatching(limits)
    private val changes = WorkBudget(limits.maximumPatchOperations.toLong(), "maximumPatchOperations")
    private val work = WorkBudget(limits.maximumNodes, "maximumNodes")
    private val depth = NodeCount(changes, limits.maximumDepth)

    fun apply(patch: YamlNode): JsonPatchEdit = when (val root = SyntaxRoots.of(tree, document)) {
        is GraphStep.Refused -> JsonPatchEdit.Failed(0, root.reason)
        is GraphStep.Done -> bounded(patch, root.node)
    }

    private fun bounded(patch: YamlNode, root: YamlNode): JsonPatchEdit = try {
        val expected = expectation.merged(root, patch)
        finished(Edits(root).merged(tree, JsonPointer.ROOT, patch), expected)
    } catch (expected: YamlDocumentLimitException) {
        JsonPatchEdit.Failed(0, JsonPatchRefusal.LimitExceeded)
    }

    private fun finished(step: SyntaxStep, expected: YamlNode): JsonPatchEdit = when (step) {
        is SyntaxStep.Done -> SyntaxVerification(tree, document).finished(SyntaxState(step.tree, expected), 0, limits)
        is SyntaxStep.Refused -> JsonPatchEdit.Failed(0, step.reason)
    }

    private inner class Edits(private val root: YamlNode) {
        fun merged(current: YamlSyntaxTree, pointer: JsonPointer, patch: YamlNode): SyntaxStep {
            depth.checkDepth(pointer.tokens.size)
            val isMerge = root.at(pointer) is YamlNode.Mapping
            val members = (patch as? YamlNode.Mapping)?.takeIf { isMerge } ?: return replaced(current, pointer, patch)
            val initial: SyntaxStep = SyntaxStep.Done(current)
            return members.entries.fold(initial) { step, member ->
                step.then { edited -> withMember(edited, pointer, member) }
            }
        }

        private fun withMember(current: YamlSyntaxTree, pointer: JsonPointer, member: YamlEntry): SyntaxStep {
            val key = (member.key as? YamlNode.Scalar)?.value ?: return SyntaxStep.Refused(JsonPatchRefusal.Unsupported)
            return memberChanged(current, pointer.child(key), member.value)
        }

        private fun memberChanged(current: YamlSyntaxTree, pointer: JsonPointer, value: YamlNode): SyntaxStep {
            if (value.isNull) return removed(current, pointer)
            return nestedOrReplaced(current, pointer, value)
        }

        private fun nestedOrReplaced(current: YamlSyntaxTree, pointer: JsonPointer, value: YamlNode): SyntaxStep {
            val isNested = value is YamlNode.Mapping && root.at(pointer) is YamlNode.Mapping
            if (isNested) return merged(current, pointer, value)
            return replaced(current, pointer, value)
        }

        private fun removed(current: YamlSyntaxTree, pointer: JsonPointer): SyntaxStep {
            if (root.at(pointer) == null) return SyntaxStep.Done(current)
            changes.charge()
            return SyntaxEdits(tree = current, document = document, root = root, budget = work).removed(pointer)
        }

        private fun replaced(current: YamlSyntaxTree, pointer: JsonPointer, patch: YamlNode): SyntaxStep {
            changes.charge()
            val value = values.merged(root.at(pointer), patch)
            return SyntaxEdits(tree = current, document = document, root = root, budget = work).added(pointer, value)
        }
    }
}
