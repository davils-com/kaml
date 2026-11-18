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

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.document.patch.JsonPatchOperation
import com.davils.kaml.document.pointer.JsonPointer

internal object OperationNotation {
    private const val VALUE_FIELDS: String = """a valid "path" and a "value""""
    private const val FROM_FIELDS: String = """a valid "from" and "path""""

    private val readers: Map<String, Reader> = mapOf(
        "add" to Reader(VALUE_FIELDS) { fields -> withValue(fields, JsonPatchOperation::Add) },
        "remove" to Reader("a valid \"path\"") { fields -> fields.path?.let(JsonPatchOperation::Remove) },
        "replace" to Reader(VALUE_FIELDS) { fields -> withValue(fields, JsonPatchOperation::Replace) },
        "move" to Reader(FROM_FIELDS) { fields -> withFrom(fields, JsonPatchOperation::Move) },
        "copy" to Reader(FROM_FIELDS) { fields -> withFrom(fields, JsonPatchOperation::Copy) },
        "test" to Reader(VALUE_FIELDS) { fields -> withValue(fields, JsonPatchOperation::Test) },
    )

    fun read(item: YamlNode): OperationReading {
        val name = item["op"].string() ?: return OperationReading.Invalid("an operation needs an \"op\"")
        return readNamed(name, item)
    }

    private fun readNamed(name: String, item: YamlNode): OperationReading {
        val reader = readers[name] ?: return OperationReading.Invalid("the operation is not known")
        val operation = reader.create(OperationFields(item))
        return operation?.let(OperationReading::Read) ?: OperationReading.Invalid("the operation needs ${reader.needs}")
    }

    private fun withValue(
        fields: OperationFields,
        create: (JsonPointer, YamlNode) -> JsonPatchOperation,
    ): JsonPatchOperation? {
        val path = fields.path ?: return null
        return fields.value?.let { value -> create(path, value) }
    }

    private fun withFrom(
        fields: OperationFields,
        create: (JsonPointer, JsonPointer) -> JsonPatchOperation,
    ): JsonPatchOperation? {
        val from = fields.from ?: return null
        return fields.path?.let { path -> create(from, path) }
    }

    private data class Reader(val needs: String, val create: (OperationFields) -> JsonPatchOperation?)
}
