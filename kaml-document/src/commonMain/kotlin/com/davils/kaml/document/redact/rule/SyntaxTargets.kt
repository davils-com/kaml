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

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.YamlStream
import com.davils.kaml.core.syntax.YamlSyntaxNode
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.ast.YamlEntrySyntax
import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kaml.document.internal.NodeSet
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.internal.identityOrdinals
import com.davils.kaml.document.patch.syntax.ContentRanges
import com.davils.kore.core.text.TextRange

internal class SyntaxTargets(
    private val tree: YamlSyntaxTree,
    private val rules: RedactionRules,
    private val budget: WorkBudget,
) {
    fun graphNodes(stream: YamlStream): List<YamlSyntaxNode>? {
        val ordinalOf = identityOrdinals()
        val targets = Targets(rules, budget, ordinalOf)
        val ranges = ArrayList<TextRange?>()
        stream.documents.forEach { document ->
            val seen = NodeSet(ordinalOf)
            targets.of(document.root).nodes.forEach { target -> ranges.addAll(definitionsOf(target, targets, seen)) }
        }
        val known = ranges.filterNotNull()
        val nonEmpty = known.filter { range -> !range.isEmpty }
        return nonEmpty.map(tree.root::coveringNode).takeIf { known.size == ranges.size }
    }

    private fun definitionsOf(target: YamlNode, targets: Targets, seen: NodeSet): List<TextRange?> {
        val definitions = ArrayList<TextRange?>()
        targets.subtree(target, seen) { node ->
            val range = node.range
            val isInside = range != null && definitions.any { outer -> outer != null && outer.contains(range) }
            if (!isInside) definitions.add(range)
        }
        return definitions
    }

    private fun TextRange.contains(other: TextRange): Boolean = start <= other.start && other.end <= end

    fun keyedNodes(walk: SyntaxWalk): List<YamlSyntaxNode> {
        if (!rules.hasKeys) return emptyList()
        val entries = walk.mappings.flatMap(YamlValueSyntax.Mapping::entries)
        return entries.filter { entry -> rules.isSecretKey(tokenOf(entry)) }.mapNotNull(::valueNodeOf)
    }

    fun duplicateNodes(walk: SyntaxWalk, covered: Coverage): List<YamlSyntaxNode> = walk.mappings.flatMap { mapping ->
        val groups = mapping.entries
            .groupBy(::tokenOf)
            .values
            .filter { group -> group.size > 1 }
        groups.flatMap { group ->
            val nodes = group.mapNotNull(::valueNodeOf)
            nodes.takeIf { nodes.any { node -> covered.contains(ContentRanges.of(node)) } }.orEmpty()
        }
    }

    private fun valueNodeOf(entry: YamlEntrySyntax): YamlSyntaxNode? {
        val value = entry.value?.takeIf { it !is YamlValueSyntax.Empty } ?: return null
        return value.node.takeIf { node -> !ContentRanges.of(node).isEmpty }
    }

    private fun tokenOf(entry: YamlEntrySyntax): String = entry.keyText ?: kindOf(entry.key)

    private fun kindOf(key: YamlValueSyntax): String = when (key) {
        is YamlValueSyntax.Sequence -> SEQUENCE
        is YamlValueSyntax.Mapping -> MAPPING
        is YamlValueSyntax.Scalar, is YamlValueSyntax.Alias, is YamlValueSyntax.Empty -> ""
    }

    private companion object {
        const val SEQUENCE: String = "Sequence"
        const val MAPPING: String = "Mapping"
    }
}
