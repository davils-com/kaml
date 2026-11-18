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
package com.davils.kaml.core.node

internal class NodeEquality {
    private val proven = HashSet<NodePair>()

    private val comparison = DeepRecursiveFunction<NodePair, Boolean> { pair -> isSame(pair) }

    fun isSame(first: YamlNode, second: YamlNode): Boolean = comparison(NodePair(first = first, second = second))

    private suspend fun DeepRecursiveScope<NodePair, Boolean>.isSame(pair: NodePair): Boolean {
        val isKnown = pair.first === pair.second || pair in proven
        return isKnown || isCompared(pair)
    }

    private suspend fun DeepRecursiveScope<NodePair, Boolean>.isCompared(pair: NodePair): Boolean {
        val first = pair.first
        val second = pair.second
        val isSame = first.hashCode() == second.hashCode() && hasSameContent(first = first, second = second)
        if (isSame) proven.add(pair)
        return isSame
    }

    private suspend fun DeepRecursiveScope<NodePair, Boolean>.hasSameContent(
        first: YamlNode,
        second: YamlNode,
    ): Boolean {
        val isSameKind = first.kind == second.kind && first.tag == second.tag
        if (!isSameKind) return false
        return when (first) {
            is YamlNode.Scalar -> first == second
            is YamlNode.Sequence -> hasSameItems(first = first, second = second as YamlNode.Sequence)
            is YamlNode.Mapping -> hasSameEntries(first = first, second = second as YamlNode.Mapping)
        }
    }

    private suspend fun DeepRecursiveScope<NodePair, Boolean>.hasSameItems(
        first: YamlNode.Sequence,
        second: YamlNode.Sequence,
    ): Boolean {
        val isSameSize = first.items.size == second.items.size
        val pairs = first.items.zip(second.items)
        return isSameSize && pairs.all { (left, right) -> isSamePair(first = left, second = right) }
    }

    private suspend fun DeepRecursiveScope<NodePair, Boolean>.hasSameEntries(
        first: YamlNode.Mapping,
        second: YamlNode.Mapping,
    ): Boolean {
        val isSameSize = first.entries.size == second.entries.size
        val ranks = first.entries.indices
        return isSameSize && ranks.all { rank -> isSameRank(first = first, second = second, rank = rank) }
    }

    private suspend fun DeepRecursiveScope<NodePair, Boolean>.isSameRank(
        first: YamlNode.Mapping,
        second: YamlNode.Mapping,
        rank: Int,
    ): Boolean = isSameEntry(entry = first.entryOfKeyRank(rank), candidate = second.entryOfKeyRank(rank))

    private suspend fun DeepRecursiveScope<NodePair, Boolean>.isSameEntry(
        entry: YamlEntry,
        candidate: YamlEntry,
    ): Boolean {
        val isSameKey = isSamePair(first = entry.key, second = candidate.key)
        return isSameKey && isSamePair(first = entry.value, second = candidate.value)
    }

    private suspend fun DeepRecursiveScope<NodePair, Boolean>.isSamePair(first: YamlNode, second: YamlNode): Boolean {
        val isLeaf = first is YamlNode.Scalar || second is YamlNode.Scalar
        if (isLeaf) return first is YamlNode.Scalar && first == second
        return callRecursive(NodePair(first = first, second = second))
    }
}
