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

import com.davils.kaml.core.YamlDuplicateKeys
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.YamlStream
import com.davils.kaml.core.syntax.YamlSyntaxNode
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.ast.YamlEntrySyntax
import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kaml.document.internal.NodeSet
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.internal.identityOrdinals
import com.davils.kore.core.text.TextRange

private const val MERGE_KEY: String = "<<"

internal class DroppedEntries(tree: YamlSyntaxTree, stream: YamlStream, walk: SyntaxWalk, budget: WorkBudget) {
    private val isLastKept = tree.yaml.configuration.duplicateKeys == YamlDuplicateKeys.KeepLast
    private val anchors = AnchorDefinitions(tree)

    val nodes: List<YamlSyntaxNode>

    val unmatched: List<TextRange>

    private val found = ArrayList<YamlSyntaxNode>()
    private val merged = ArrayList<TextRange>()

    init {
        val composed = composedMappings(stream, budget)
        val missing = ArrayList<TextRange>()
        walk.mappings.forEach { mapping ->
            budget.charge(mapping.entries.size.toLong())
            val counterpart = composed[mapping.node.range]
            counterpart?.let { match -> collect(mapping, match) } ?: missing.add(mapping.node.range)
        }
        val mergeSources = Coverage(merged)
        nodes = found
        unmatched = missing.filter { range -> !mergeSources.contains(range) }
    }

    private fun composedMappings(stream: YamlStream, budget: WorkBudget): Map<TextRange, YamlNode.Mapping> {
        val mappings = HashMap<TextRange, YamlNode.Mapping>()
        val seen = NodeSet(identityOrdinals())
        val pending = ArrayDeque<YamlNode>()
        stream.documents.forEach { document -> pending.addLast(document.root) }
        while (pending.isNotEmpty()) {
            val node = pending.removeLast()
            if (seen.add(node)) {
                budget.charge()
                indexed(node, mappings)
                childrenOf(node).forEach(pending::addLast)
            }
        }
        return mappings
    }

    private fun indexed(node: YamlNode, mappings: MutableMap<TextRange, YamlNode.Mapping>) {
        val mapping = node as? YamlNode.Mapping ?: return
        mapping.range?.let { range -> mappings[range] = mapping }
    }

    private fun childrenOf(node: YamlNode): List<YamlNode> = when (node) {
        is YamlNode.Scalar -> emptyList()
        is YamlNode.Sequence -> node.items
        is YamlNode.Mapping -> node.entries.flatMap { entry -> listOf(entry.key, entry.value) }
    }

    private fun collect(mapping: YamlValueSyntax.Mapping, counterpart: YamlNode.Mapping): Boolean {
        val keyRanges = counterpart.entries.mapNotNullTo(HashSet()) { entry -> entry.key.range }
        val candidates = mapping.entries.filter { entry -> resolvedRangeOf(entry) in keyRanges }
        val groups = candidates.groupBy(::resolvedRangeOf).values
        val kept = groups.mapTo(HashSet()) { group -> group.last().takeIf { isLastKept } ?: group.first() }
        val unkept = mapping.entries.filter { entry -> entry !in kept }
        val (merges, others) = unkept.partition { entry -> isMerge(entry, keyRanges) }
        merges.mapNotNullTo(merged, ::valueRangeOf)
        return found.addAll(others.flatMap(::partsOf))
    }

    private fun resolvedRangeOf(entry: YamlEntrySyntax): TextRange {
        val key = entry.key
        val alias = key as? YamlValueSyntax.Alias ?: return key.node.range
        return anchors.rangeOf(alias.name, alias.node.range.start) ?: alias.node.range
    }

    private fun valueRangeOf(entry: YamlEntrySyntax): TextRange? {
        val value = entry.value ?: return null
        return value.node.range
    }

    private fun isMerge(entry: YamlEntrySyntax, keyRanges: Set<TextRange>): Boolean {
        val keyText = entry.key.node.text
        val isMergeText = entry.key is YamlValueSyntax.Scalar && keyText.trim() == MERGE_KEY
        return isMergeText && resolvedRangeOf(entry) !in keyRanges
    }

    private fun partsOf(entry: YamlEntrySyntax): List<YamlSyntaxNode> {
        val value = entry.value?.takeIf { it !is YamlValueSyntax.Empty }
        val key = entry.key
        val isCollectionKey = key is YamlValueSyntax.Mapping || key is YamlValueSyntax.Sequence
        return listOfNotNull(value?.node, key.node.takeIf { isCollectionKey })
    }
}
