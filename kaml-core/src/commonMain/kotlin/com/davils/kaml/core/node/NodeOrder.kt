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

import com.davils.kaml.core.node.internal.ScalarValue
import kotlin.reflect.KClass

internal class NodeOrder private constructor() {
    private val proven = HashSet<NodePair>()

    private val ordering = DeepRecursiveFunction<NodePair, Int> { pair -> orderOf(pair) }

    private suspend fun DeepRecursiveScope<NodePair, Int>.orderOf(pair: NodePair): Int {
        val first = pair.first
        val second = pair.second
        val shallow = shallowOrder(first, second)
        val isDecided = shallow != 0 || first === second || first is YamlNode.Scalar
        if (isDecided || pair in proven) return shallow
        val deep = deepOrder(first, second)
        if (deep == 0) proven.add(pair)
        return deep
    }

    private suspend fun DeepRecursiveScope<NodePair, Int>.deepOrder(first: YamlNode, second: YamlNode): Int =
        when (first) {
            is YamlNode.Scalar -> 0
            is YamlNode.Sequence -> itemOrder(first = first, second = second as YamlNode.Sequence)
            is YamlNode.Mapping -> entryOrder(first = first, second = second as YamlNode.Mapping)
        }

    private suspend fun DeepRecursiveScope<NodePair, Int>.itemOrder(
        first: YamlNode.Sequence,
        second: YamlNode.Sequence,
    ): Int {
        var order = 0
        var index = 0
        while (order == 0 && index < first.items.size) {
            order = childOrder(first = first.items[index], second = second.items[index])
            index++
        }
        return order
    }

    private suspend fun DeepRecursiveScope<NodePair, Int>.entryOrder(
        first: YamlNode.Mapping,
        second: YamlNode.Mapping,
    ): Int {
        var order = 0
        var rank = 0
        while (order == 0 && rank < first.entries.size) {
            val left = first.entryOfKeyRank(rank)
            val right = second.entryOfKeyRank(rank)
            val byKey = childOrder(first = left.key, second = right.key)
            order = byKey.takeIf { it != 0 } ?: childOrder(first = left.value, second = right.value)
            rank++
        }
        return order
    }

    private suspend fun DeepRecursiveScope<NodePair, Int>.childOrder(first: YamlNode, second: YamlNode): Int {
        val shallow = shallowOrder(first, second)
        val isDeep = shallow == 0 && first !== second && first !is YamlNode.Scalar
        if (!isDeep) return shallow
        return callRecursive(NodePair(first = first, second = second))
    }

    companion object {
        private val scalarValueKinds: List<KClass<out ScalarValue>> = listOf(
            ScalarValue.Text::class,
            ScalarValue.Integer::class,
            ScalarValue.Real::class,
            ScalarValue.Truth::class,
            ScalarValue.Null::class,
        )

        fun compare(first: YamlNode, second: YamlNode): Int {
            val shallow = shallowOrder(first, second)
            val isDeep = shallow == 0 && first !== second && first !is YamlNode.Scalar
            if (!isDeep) return shallow
            return NodeOrder().ordering(NodePair(first = first, second = second))
        }

        private fun shallowOrder(first: YamlNode, second: YamlNode): Int {
            if (first === second) return 0
            val byHash = first.hashCode().compareTo(second.hashCode())
            val byKind = byHash.takeIf { it != 0 } ?: first.kind.compareTo(second.kind)
            val byTag = byKind.takeIf { it != 0 } ?: first.tag.uri.compareTo(second.tag.uri)
            return byTag.takeIf { it != 0 } ?: contentOrder(first, second)
        }

        private fun contentOrder(first: YamlNode, second: YamlNode): Int = when (first) {
            is YamlNode.Scalar -> valueOrder(first = first.canonical, second = (second as YamlNode.Scalar).canonical)
            is YamlNode.Sequence -> first.items.size.compareTo((second as YamlNode.Sequence).items.size)
            is YamlNode.Mapping -> first.entries.size.compareTo((second as YamlNode.Mapping).entries.size)
        }

        private fun valueOrder(first: ScalarValue, second: ScalarValue): Int {
            val byKind = scalarValueKinds.indexOf(first::class).compareTo(scalarValueKinds.indexOf(second::class))
            return byKind.takeIf { it != 0 } ?: sameKindOrder(first, second)
        }

        private fun sameKindOrder(first: ScalarValue, second: ScalarValue): Int = when (first) {
            is ScalarValue.Text -> first.text.compareTo((second as ScalarValue.Text).text)
            is ScalarValue.Integer -> first.decimal.compareTo((second as ScalarValue.Integer).decimal)
            is ScalarValue.Real -> first.value.compareTo((second as ScalarValue.Real).value)
            is ScalarValue.Truth -> first.isTrue.compareTo((second as ScalarValue.Truth).isTrue)
            ScalarValue.Null -> 0
        }
    }
}
