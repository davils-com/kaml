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

package com.davils.kaml.document.merge.trace

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.isNull
import com.davils.kaml.document.internal.NodeCount
import com.davils.kaml.document.merge.YamlDeletion
import com.davils.kaml.document.patch.graph.GraphNodes

private const val DELETE: String = "!delete"

internal class DeletionMarkers(private val deletion: YamlDeletion, private val count: NodeCount) {
    fun isDeletion(node: YamlNode): Boolean = when (deletion) {
        YamlDeletion.None -> false
        YamlDeletion.Tag -> node.tag.uri == DELETE
        YamlDeletion.Null -> node.isNull
    }

    fun stripped(node: YamlNode, depth: Int): YamlNode {
        count.charge(node, depth)
        return strippedCounted(node)
    }

    private fun strippedCounted(node: YamlNode): YamlNode = when (node) {
        is YamlNode.Scalar -> node
        is YamlNode.Sequence -> GraphNodes.withItems(node, strippedItems(node), node.anchor)
        is YamlNode.Mapping -> GraphNodes.withEntries(node, strippedEntries(node), node.anchor)
    }

    private fun strippedItems(sequence: YamlNode.Sequence): List<YamlNode> {
        val kept = sequence.items.filterNot(::isDeletion)
        return kept.map(::strippedCounted)
    }

    private fun strippedEntries(mapping: YamlNode.Mapping): List<YamlEntry> {
        val kept = mapping.entries.filterNot { entry -> isDeletion(entry.value) }
        return kept.map { entry -> YamlEntry(key = entry.key, value = strippedCounted(entry.value)) }
    }
}
