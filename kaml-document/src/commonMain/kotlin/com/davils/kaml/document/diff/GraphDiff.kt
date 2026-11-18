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
        between(source, target, JsonPointer.ROOT)
        return found
    }

    fun between(source: YamlNode, target: YamlNode, pointer: JsonPointer) {
        budget.charge()
        if (pointer.tokens.size > limits.maximumDepth) throw YamlDocumentLimitException("maximumDepth")
        if (source == target) return
        val isSameTag = source.tag == target.tag
        val isNested = isSameTag && isNestedBetween(source, target, pointer)
        if (!isNested) add(JsonPatchOperation.Replace(pointer, target))
    }

    private fun isNestedBetween(source: YamlNode, target: YamlNode, pointer: JsonPointer): Boolean {
        val isMapping = MappingDiff.between(diff = this, source = source, target = target, pointer = pointer)
        return isMapping || SequenceDiff.between(diff = this, source = source, target = target, pointer = pointer)
    }

    fun add(operation: JsonPatchOperation) {
        if (found.size >= limits.maximumPatchOperations) throw YamlDocumentLimitException("maximumPatchOperations")
        found.add(operation)
    }
}
