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

package com.davils.kaml.document.patch.notation

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.document.patch.JsonPatch
import com.davils.kaml.document.patch.JsonPatchOperation
import com.davils.kaml.document.patch.JsonPatchReading

internal object PatchNotation {
    private const val VALUE: String = "value"
    private const val FROM: String = "from"

    fun read(node: YamlNode, limits: YamlLimits): JsonPatchReading {
        val sequence = node as? YamlNode.Sequence
            ?: return JsonPatchReading.Invalid(-1, "a patch must be a sequence of operations")
        return readBounded(sequence, limits)
    }

    fun write(operations: List<JsonPatchOperation>): YamlNode = yamlSequence(operations.map(::written))

    private fun readBounded(sequence: YamlNode.Sequence, limits: YamlLimits): JsonPatchReading {
        if (sequence.items.size > limits.maximumPatchOperations) {
            return JsonPatchReading.Invalid(-1, "a patch must not have more operations than its limits allow")
        }
        val operations = ArrayList<JsonPatchOperation>()
        var invalid: JsonPatchReading.Invalid? = null
        var index = 0
        while (invalid == null && index < sequence.items.size) {
            when (val reading = OperationNotation.read(sequence.items[index], limits)) {
                is OperationReading.Read -> operations.add(reading.operation)
                is OperationReading.Invalid -> invalid = JsonPatchReading.Invalid(index, reading.reason)
            }
            index++
        }
        return invalid ?: JsonPatchReading.Read(JsonPatch(operations, limits))
    }

    private fun written(operation: JsonPatchOperation): YamlNode {
        val head = listOf("op" to yamlScalar(nameOf(operation)), "path" to yamlScalar(operation.path.text))
        val fields = head + extrasOf(operation)
        return yamlMapping(fields.map { (key, value) -> YamlEntry(yamlScalar(key), value) })
    }

    private fun nameOf(operation: JsonPatchOperation): String = when (operation) {
        is JsonPatchOperation.Add -> "add"
        is JsonPatchOperation.Remove -> "remove"
        is JsonPatchOperation.Replace -> "replace"
        is JsonPatchOperation.Move -> "move"
        is JsonPatchOperation.Copy -> "copy"
        is JsonPatchOperation.Test -> "test"
    }

    private fun extrasOf(operation: JsonPatchOperation): List<Pair<String, YamlNode>> = when (operation) {
        is JsonPatchOperation.Add -> listOf(VALUE to operation.value)
        is JsonPatchOperation.Remove -> emptyList()
        is JsonPatchOperation.Replace -> listOf(VALUE to operation.value)
        is JsonPatchOperation.Move -> listOf(FROM to yamlScalar(operation.from.text))
        is JsonPatchOperation.Copy -> listOf(FROM to yamlScalar(operation.from.text))
        is JsonPatchOperation.Test -> listOf(VALUE to operation.value)
    }
}
