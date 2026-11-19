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
import com.davils.kaml.document.internal.Members
import com.davils.kaml.document.internal.NodeSet
import com.davils.kaml.document.internal.OrderedGroups
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kaml.document.pointer.PointerTokens

internal class Targets(
    private val rules: RedactionRules,
    private val budget: WorkBudget,
    private val ordinalOf: (YamlNode) -> Int,
) {
    private val memberIndexes = HashMap<Int, OrderedGroups<String, YamlNode>>()

    fun of(root: YamlNode): NodeSet {
        val targets = NodeSet(ordinalOf)
        val roots = queryRoots(root)
        rules.paths.forEach { path ->
            roots.forEach { start ->
                path.query(start).forEach { match -> allAt(start, match.location).forEach(targets::add) }
            }
        }
        if (rules.hasKeys) keyed(root, targets)
        return targets
    }

    private fun queryRoots(root: YamlNode): List<YamlNode> {
        val roots = NodeSet(ordinalOf)
        roots.add(root)
        if (rules.paths.isNotEmpty()) subtree(root) { node -> collectionKeysOf(node).forEach(roots::add) }
        return roots.nodes
    }

    private fun collectionKeysOf(node: YamlNode): List<YamlNode> {
        val entries = (node as? YamlNode.Mapping)?.entries.orEmpty()
        return entries.map(YamlEntry::key).filter { key -> key !is YamlNode.Scalar }
    }

    fun subtree(node: YamlNode, seen: NodeSet = NodeSet(ordinalOf), visit: (YamlNode) -> Unit) {
        val pending = ArrayDeque<YamlNode>()
        pending.addLast(node)
        while (pending.isNotEmpty()) {
            val current = pending.removeLast()
            if (seen.add(current)) {
                budget.charge()
                visit(current)
                childrenOf(current).forEach(pending::addLast)
            }
        }
    }

    private fun keyed(root: YamlNode, targets: NodeSet) {
        subtree(root) { node ->
            val entries = (node as? YamlNode.Mapping)?.entries.orEmpty()
            entries.forEach { entry ->
                if (rules.isSecretKey(Members.tokenOf(entry.key))) targets.add(entry.value)
            }
        }
    }

    private fun allAt(root: YamlNode, pointer: JsonPointer): List<YamlNode> {
        var current = listOf(root)
        pointer.tokens.forEach { token ->
            current = current.flatMap { node -> childrenNamed(node, token) }
            budget.charge(current.size.toLong() + 1L)
        }
        return current
    }

    private fun childrenNamed(node: YamlNode, token: String): List<YamlNode> = when (node) {
        is YamlNode.Scalar -> {
            emptyList()
        }

        is YamlNode.Sequence -> {
            listOfNotNull(PointerTokens.indexOf(token)?.let(node.items::getOrNull))
        }

        is YamlNode.Mapping -> {
            membersOf(node)[token].orEmpty()
        }
    }

    private fun membersOf(mapping: YamlNode.Mapping): OrderedGroups<String, YamlNode> =
        memberIndexes.getOrPut(ordinalOf(mapping)) {
            budget.charge(mapping.entries.size.toLong())
            val members = OrderedGroups<String, YamlNode>()
            mapping.entries.forEach { entry -> members.add(Members.tokenOf(entry.key), entry.value) }
            members
        }

    private fun childrenOf(node: YamlNode): List<YamlNode> = when (node) {
        is YamlNode.Scalar -> emptyList()
        is YamlNode.Sequence -> node.items
        is YamlNode.Mapping -> node.entries.flatMap { entry -> listOf(entry.key, entry.value) }
    }
}
