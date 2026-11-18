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

package com.davils.kaml.core.compose

import com.davils.kaml.core.node.YamlNode

internal object NodeSizes {
    fun of(node: YamlNode, most: Long): Long {
        val pending = ArrayList<YamlNode>()
        pending.add(node)
        var count = 0L
        while (pending.isNotEmpty() && count <= most) {
            count++
            enqueueChildren(pending.removeAt(pending.lastIndex), pending)
        }
        return count
    }

    private fun enqueueChildren(node: YamlNode, pending: MutableList<YamlNode>) {
        when (node) {
            is YamlNode.Scalar -> Unit

            is YamlNode.Sequence -> pending.addAll(node.items)

            is YamlNode.Mapping -> node.entries.forEach { entry ->
                pending.add(entry.key)
                pending.add(entry.value)
            }
        }
    }
}
