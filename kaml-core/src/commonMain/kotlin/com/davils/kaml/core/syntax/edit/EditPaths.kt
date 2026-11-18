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

package com.davils.kaml.core.syntax.edit

import com.davils.kaml.core.syntax.ast.YamlEntrySyntax
import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kaml.core.syntax.edit.YamlPath.Segment

internal object EditPaths {
    fun locate(root: YamlValueSyntax, path: YamlPath): Target? {
        val segment = path.last ?: return null
        val steps = path.segments.dropLast(1)
        val parent = steps.fold<Segment, YamlValueSyntax?>(root) { current, step ->
            current?.let { child(it, step) }
        }
        return parent?.let { Target(it, segment) }
    }

    fun entryOf(parent: YamlValueSyntax, segment: Segment): YamlEntrySyntax? {
        val key = segment as? Segment.Key ?: return null
        return entryOf(parent, key.name)
    }

    private fun child(value: YamlValueSyntax, segment: Segment): YamlValueSyntax? = when (segment) {
        is Segment.Key -> entryOf(value, segment.name)?.value
        is Segment.Index -> itemOf(value, segment.index)
    }

    private fun entryOf(parent: YamlValueSyntax, name: String): YamlEntrySyntax? {
        val mapping = parent as? YamlValueSyntax.Mapping ?: return null
        return mapping.entry(name)
    }

    private fun itemOf(parent: YamlValueSyntax, index: Int): YamlValueSyntax? {
        val sequence = parent as? YamlValueSyntax.Sequence ?: return null
        return sequence.items.getOrNull(index)
    }

    internal data class Target(val parent: YamlValueSyntax, val segment: Segment)
}
