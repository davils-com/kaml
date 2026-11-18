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

package com.davils.kaml.document.redact.rule

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.internal.NodeKey
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.patch.graph.GraphNodes

internal class GraphRedaction(private val rules: RedactionRules) {
    private val budget = WorkBudget(rules.limits.maximumNodes, "maximumNodes")
    private val rewritten = HashMap<NodeKey, YamlNode>()

    fun redacted(root: YamlNode): YamlNode {
        val targets = Targets(rules, budget).of(root)
        return rewrittenOf(root, targets, 0)
    }

    private fun rewrittenOf(node: YamlNode, targets: Set<NodeKey>, depth: Int): YamlNode {
        val key = NodeKey(node)
        if (key in targets) return yamlScalar(rules.replacement)
        return rewritten.getOrPut(key) { changed(node, targets, depth) }
    }

    private fun changed(node: YamlNode, targets: Set<NodeKey>, depth: Int): YamlNode {
        budget.charge()
        if (depth > rules.limits.maximumDepth) throw YamlDocumentLimitException("maximumDepth")
        return when (node) {
            is YamlNode.Scalar -> scalar(node)
            is YamlNode.Sequence -> sequence(node, targets, depth + 1)
            is YamlNode.Mapping -> mapping(node, targets, depth + 1)
        }
    }

    private fun sequence(node: YamlNode.Sequence, targets: Set<NodeKey>, depth: Int): YamlNode {
        val items = node.items.map { item -> rewrittenOf(item, targets, depth) }
        val isSame = items.indices.all { index -> items[index] === node.items[index] }
        return node.takeIf { isSame } ?: GraphNodes.withItems(node, items, node.anchor)
    }

    private fun mapping(node: YamlNode.Mapping, targets: Set<NodeKey>, depth: Int): YamlNode {
        val entries = node.entries.map { entry ->
            val key = entry.key.takeIf { it is YamlNode.Scalar } ?: rewrittenOf(entry.key, targets, depth)
            YamlEntry(key, rewrittenOf(entry.value, targets, depth))
        }
        val isSame = entries.indices.all { index ->
            entries[index].key === node.entries[index].key && entries[index].value === node.entries[index].value
        }
        return node.takeIf { isSame } ?: GraphNodes.withEntries(node, unique(entries), node.anchor)
    }

    private fun unique(entries: List<YamlEntry>): List<YamlEntry> {
        val keys = entries.mapTo(HashSet(), YamlEntry::key)
        check(keys.size == entries.size) { "The redacted graph would hold a key of a mapping twice" }
        return entries
    }

    private fun scalar(node: YamlNode.Scalar): YamlNode {
        val redacted = rules.value(node.value) ?: throw YamlDocumentLimitException("maximumPatternInputLength")
        if (redacted == node.value) return node
        return yamlScalar(redacted)
    }
}
