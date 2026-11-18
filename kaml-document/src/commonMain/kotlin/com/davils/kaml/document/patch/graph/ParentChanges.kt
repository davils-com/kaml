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

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.document.internal.Members
import com.davils.kaml.document.patch.JsonPatchRefusal
import com.davils.kaml.document.pointer.PointerSteps
import com.davils.kaml.document.pointer.PointerTokens

internal object ParentChanges {
    private const val END: String = "-"

    fun added(parent: YamlNode, token: String, value: YamlNode): GraphStep = when (parent) {
        is YamlNode.Scalar -> GraphStep.Refused(JsonPatchRefusal.NotAContainer)
        is YamlNode.Sequence -> inserted(parent, token, value)
        is YamlNode.Mapping -> GraphStep.Done(withValue(parent, token, value))
    }

    fun removed(parent: YamlNode, token: String): GraphStep = when (parent) {
        is YamlNode.Scalar -> GraphStep.Refused(JsonPatchRefusal.NotAContainer)
        is YamlNode.Sequence -> withoutItem(parent, token)
        is YamlNode.Mapping -> withoutEntry(parent, token)
    }

    fun replaced(parent: YamlNode, token: String, value: YamlNode): GraphStep {
        if (PointerSteps.child(parent, token) == null) return GraphStep.Refused(JsonPatchRefusal.MissingTarget)
        return GraphStep.Done(GraphNodes.withChild(parent, token, value))
    }

    private fun inserted(sequence: YamlNode.Sequence, token: String, value: YamlNode): GraphStep {
        val items = sequence.items
        val position = PointerTokens.indexOf(token)?.takeIf { it <= items.size }
        val index = items.size.takeIf { token == END } ?: position
            ?: return GraphStep.Refused(JsonPatchRefusal.InvalidIndex)
        val changed = buildList {
            addAll(items.subList(fromIndex = 0, toIndex = index))
            add(value)
            addAll(items.subList(fromIndex = index, toIndex = items.size))
        }
        return GraphStep.Done(GraphNodes.withItems(sequence, changed, anchor = null))
    }

    private fun withValue(mapping: YamlNode.Mapping, token: String, value: YamlNode): YamlNode {
        val isPresent = Members.first(mapping, token) != null
        if (isPresent) return GraphNodes.withChild(mapping, token, value)
        val appended = buildList {
            addAll(mapping.entries)
            add(YamlEntry(yamlScalar(token), value))
        }
        return GraphNodes.withEntries(mapping, appended, anchor = null)
    }

    private fun withoutItem(sequence: YamlNode.Sequence, token: String): GraphStep {
        val index = PointerTokens.indexOf(token)?.takeIf { it < sequence.items.size }
            ?: return GraphStep.Refused(JsonPatchRefusal.MissingTarget)
        val items = sequence.items.filterIndexed { position, _ -> position != index }
        return GraphStep.Done(GraphNodes.withItems(sequence, items, anchor = null))
    }

    private fun withoutEntry(mapping: YamlNode.Mapping, token: String): GraphStep {
        val existing = Members.first(mapping, token) ?: return GraphStep.Refused(JsonPatchRefusal.MissingTarget)
        val entries = mapping.entries.filter { it !== existing }
        return GraphStep.Done(GraphNodes.withEntries(mapping, entries, anchor = null))
    }
}
