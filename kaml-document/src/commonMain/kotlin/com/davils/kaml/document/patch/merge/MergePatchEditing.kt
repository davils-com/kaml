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

@file:OptIn(DavilsInternal::class)

package com.davils.kaml.document.patch.merge

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.isNull
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.internal.MemberIndex
import com.davils.kaml.document.internal.NodeCount
import com.davils.kaml.document.internal.chargeOrFail
import com.davils.kaml.document.patch.JsonPatchEdit
import com.davils.kaml.document.patch.JsonPatchRefusal
import com.davils.kaml.document.patch.graph.GraphStep
import com.davils.kaml.document.patch.syntax.SyntaxEdits
import com.davils.kaml.document.patch.syntax.SyntaxRoots
import com.davils.kaml.document.patch.syntax.SyntaxState
import com.davils.kaml.document.patch.syntax.SyntaxStep
import com.davils.kaml.document.patch.syntax.SyntaxVerification
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget

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
        finished(Edits(root).merged(tree, Place(JsonPointer.ROOT, root), patch), expected)
    } catch (expected: YamlDocumentLimitException) {
        JsonPatchEdit.Failed(0, JsonPatchRefusal.LimitExceeded)
    }

    private fun finished(step: SyntaxStep, expected: YamlNode): JsonPatchEdit = when (step) {
        is SyntaxStep.Done -> SyntaxVerification(tree, document).finished(SyntaxState(step.tree, expected), 0, limits)
        is SyntaxStep.Refused -> JsonPatchEdit.Failed(0, step.reason)
    }

    private data class Place(val pointer: JsonPointer, val node: YamlNode?)

    private inner class Edits(private val root: YamlNode) {
        fun merged(current: YamlSyntaxTree, place: Place, patch: YamlNode): SyntaxStep {
            depth.checkDepth(place.pointer.tokens.size)
            val target = place.node as? YamlNode.Mapping
            val members = patch as? YamlNode.Mapping
            if (target == null || members == null) return replaced(current, place, patch)
            val index = MemberIndex(target, work)
            val initial: SyntaxStep = SyntaxStep.Done(current)
            return members.entries.fold(initial) { step, member ->
                step.then { edited ->
                    withMember(current = edited, parent = place.pointer, member = member, index = index)
                }
            }
        }

        private fun withMember(
            current: YamlSyntaxTree,
            parent: JsonPointer,
            member: YamlEntry,
            index: MemberIndex,
        ): SyntaxStep {
            val key = (member.key as? YamlNode.Scalar)?.value ?: return SyntaxStep.Refused(JsonPatchRefusal.Unsupported)
            return memberChanged(current, Place(parent.child(key), index.valueOf(key)), member.value)
        }

        private fun memberChanged(current: YamlSyntaxTree, place: Place, value: YamlNode): SyntaxStep {
            if (value.isNull) return removed(current, place)
            return nestedOrReplaced(current, place, value)
        }

        private fun nestedOrReplaced(current: YamlSyntaxTree, place: Place, value: YamlNode): SyntaxStep {
            val isNested = value is YamlNode.Mapping && place.node is YamlNode.Mapping
            if (isNested) return merged(current, place, value)
            return replaced(current, place, value)
        }

        private fun removed(current: YamlSyntaxTree, place: Place): SyntaxStep {
            if (place.node == null) return SyntaxStep.Done(current)
            changes.chargeOrFail()
            return editsOf(current).removed(place.pointer)
        }

        private fun replaced(current: YamlSyntaxTree, place: Place, patch: YamlNode): SyntaxStep {
            changes.chargeOrFail()
            val value = values.merged(place.node, patch)
            return editsOf(current).added(place.pointer, value)
        }

        private fun editsOf(current: YamlSyntaxTree): SyntaxEdits =
            SyntaxEdits(tree = current, document = document, root = root, budget = work)
    }
}
