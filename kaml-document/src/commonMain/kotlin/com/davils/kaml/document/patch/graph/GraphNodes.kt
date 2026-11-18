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

import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.document.internal.Members
import com.davils.kaml.document.pointer.PointerTokens

internal object GraphNodes {
    fun withEntries(mapping: YamlNode.Mapping, entries: List<YamlEntry>, anchor: YamlAnchor?): YamlNode.Mapping =
        yamlMapping(entries = entries, style = mapping.style, anchor = anchor, tag = mapping.tag)

    fun withItems(sequence: YamlNode.Sequence, items: List<YamlNode>, anchor: YamlAnchor?): YamlNode.Sequence =
        yamlSequence(items = items, style = sequence.style, anchor = anchor, tag = sequence.tag)

    fun withChild(node: YamlNode, token: String, child: YamlNode): YamlNode = when (node) {
        is YamlNode.Scalar -> node
        is YamlNode.Sequence -> withItems(node, replacedItem(node.items, token, child), anchor = null)
        is YamlNode.Mapping -> withEntries(node, replacedEntry(node, token, child), anchor = null)
    }

    private fun replacedItem(items: List<YamlNode>, token: String, child: YamlNode): List<YamlNode> {
        val position = PointerTokens.indexOf(token)
        return items.mapIndexed { index, item -> child.takeIf { index == position } ?: item }
    }

    private fun replacedEntry(mapping: YamlNode.Mapping, token: String, child: YamlNode): List<YamlEntry> {
        val target = Members.first(mapping, token)
        return mapping.entries.map { entry ->
            YamlEntry(
                key = entry.key,
                value = child,
            ).takeIf { entry === target } ?: entry
        }
    }
}
