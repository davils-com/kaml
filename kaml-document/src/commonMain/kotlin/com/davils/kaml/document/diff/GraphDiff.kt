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

package com.davils.kaml.document.diff

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.patch.JsonPatchOperation
import com.davils.kaml.document.pointer.JsonPointer

internal class GraphDiff(private val limits: YamlLimits) {
    private val budget = WorkBudget(limits.maximumNodes, "maximumNodes")
    private val found = ArrayList<JsonPatchOperation>()

    fun operations(source: YamlNode, target: YamlNode): List<JsonPatchOperation> {
        between(source = source, target = target, pointer = JsonPointer.ROOT, isHashTrusted = true)
        return found
    }

    fun between(source: YamlNode, target: YamlNode, pointer: JsonPointer, isHashTrusted: Boolean) {
        budget.charge()
        if (pointer.tokens.size > limits.maximumDepth) throw YamlDocumentLimitException("maximumDepth")
        val isHashEqual = isHashTrusted && source.hashCode() == target.hashCode()
        val isProvenEqual = isHashEqual && source == target
        val isEqual = source === target || isProvenEqual
        if (isEqual) return
        val isSameTag = source.tag == target.tag
        val below = Step(pointer = pointer, isHashTrusted = isHashTrusted && !isHashEqual)
        val isNested = isSameTag && isNestedBetween(source, target, below)
        val isReplaced = !isNested && (isHashTrusted || source != target)
        if (isReplaced) add(JsonPatchOperation.Replace(pointer, target))
    }

    private fun isNestedBetween(source: YamlNode, target: YamlNode, step: Step): Boolean {
        val isMapping = MappingDiff.between(
            diff = this,
            source = source,
            target = target,
            pointer = step.pointer,
            isHashTrusted = step.isHashTrusted,
        )
        if (isMapping) return true
        return SequenceDiff.between(
            diff = this,
            source = source,
            target = target,
            pointer = step.pointer,
            isHashTrusted = step.isHashTrusted,
        )
    }

    fun add(operation: JsonPatchOperation) {
        if (found.size >= limits.maximumPatchOperations) throw YamlDocumentLimitException("maximumPatchOperations")
        found.add(operation)
    }

    private data class Step(val pointer: JsonPointer, val isHashTrusted: Boolean)
}
