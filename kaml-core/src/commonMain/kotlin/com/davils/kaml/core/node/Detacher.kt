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

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.internal.NodeFactory
import com.davils.kaml.core.node.internal.NodeIdentity

internal class Detacher(private val limits: YamlLimits) {
    private val copies = HashMap<NodeIdentity, YamlNode>()
    private var count = 0L

    private val detaching = DeepRecursiveFunction<YamlNode, YamlNode> { node ->
        copies[NodeIdentity(node)] ?: created(node)
    }

    fun copy(node: YamlNode): YamlNode = detaching(node)

    private suspend fun DeepRecursiveScope<YamlNode, YamlNode>.created(node: YamlNode): YamlNode {
        count++
        require(count <= limits.maximumNodes) { "A detached copy cannot have more than ${limits.maximumNodes} nodes" }
        val copy = when (node) {
            is YamlNode.Scalar -> scalar(node)
            is YamlNode.Sequence -> sequence(node)
            is YamlNode.Mapping -> mapping(node)
        }
        copies[NodeIdentity(node)] = copy
        return copy
    }

    private fun scalar(node: YamlNode.Scalar): YamlNode = NodeFactory.scalar(
        value = node.value,
        tag = node.tag,
        style = node.style,
        canonical = node.canonical,
        anchor = node.anchor,
    )

    private suspend fun DeepRecursiveScope<YamlNode, YamlNode>.sequence(node: YamlNode.Sequence): YamlNode =
        YamlNode.Sequence(
            items = node.items.map { item -> callRecursive(item) },
            tag = node.tag,
            style = node.style,
            anchor = node.anchor,
            range = null,
        )

    private suspend fun DeepRecursiveScope<YamlNode, YamlNode>.mapping(node: YamlNode.Mapping): YamlNode =
        YamlNode.Mapping(
            entries = node.entries.map { entry ->
                YamlEntry(key = callRecursive(entry.key), value = callRecursive(entry.value))
            },
            tag = node.tag,
            style = node.style,
            anchor = node.anchor,
            range = null,
        )
}
