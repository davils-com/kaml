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

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.patch.JsonPatchOperation
import com.davils.kaml.document.pointer.JsonPointer

internal object SequenceDiff {
    private const val END: String = "-"

    fun between(
        diff: GraphDiff,
        source: YamlNode,
        target: YamlNode,
        pointer: JsonPointer,
        isHashTrusted: Boolean,
    ): Boolean {
        val before = (source as? YamlNode.Sequence)?.items
        val after = (target as? YamlNode.Sequence)?.items
        if (before == null || after == null) return false
        val common = minOf(a = before.size, b = after.size)
        for (index in 0 until common) {
            diff.between(
                source = before[index],
                target = after[index],
                pointer = pointer.child(index),
                isHashTrusted = isHashTrusted,
            )
        }
        for (index in before.size - 1 downTo common) diff.add(JsonPatchOperation.Remove(pointer.child(index)))
        for (index in common until after.size) diff.add(JsonPatchOperation.Add(pointer.child(END), after[index]))
        return true
    }
}
