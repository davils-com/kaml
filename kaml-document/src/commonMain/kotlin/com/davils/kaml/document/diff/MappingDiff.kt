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

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.internal.OrderedGroups
import com.davils.kaml.document.patch.JsonPatchOperation
import com.davils.kaml.document.pointer.JsonPointer

internal object MappingDiff {
    fun between(
        diff: GraphDiff,
        source: YamlNode,
        target: YamlNode,
        pointer: JsonPointer,
        isHashTrusted: Boolean,
    ): Boolean {
        val before = membersOf(source)
        val after = membersOf(target)
        if (before == null || after == null) return false
        val removed = before.keys.filter { key -> key !in after }
        removed.forEach { key -> diff.add(JsonPatchOperation.Remove(pointer.child(key))) }
        before.forEach { key, values ->
            after[key]?.let { changed ->
                val child = pointer.child(key)
                val next = changed.first()
                diff.between(source = values.first(), target = next, pointer = child, isHashTrusted = isHashTrusted)
            }
        }
        after.forEach { key, values ->
            if (key !in before) diff.add(JsonPatchOperation.Add(pointer.child(key), values.first()))
        }
        return true
    }

    private fun membersOf(node: YamlNode): OrderedGroups<String, YamlNode>? {
        val mapping = node as? YamlNode.Mapping ?: return null
        val members = OrderedGroups<String, YamlNode>()
        mapping.entries.forEach { entry ->
            val key = (entry.key as? YamlNode.Scalar)?.takeIf { it.tag == YamlTag.STRING }
            key?.let { text -> members.add(text.value, entry.value) }
        }
        return members.takeIf { it.size == mapping.entries.size }
    }
}
