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
import com.davils.kaml.document.internal.Members
import com.davils.kaml.document.internal.NodeCount
import com.davils.kaml.document.merge.YamlMerged
import com.davils.kaml.document.patch.graph.GraphNodes
import com.davils.kaml.document.pointer.JsonPointer

internal class TracedNodes(private val count: NodeCount) {
    fun merged(traced: Traced): YamlMerged {
        val origins = LinkedHashMap<JsonPointer, String>()
        val node = built(traced, JsonPointer.ROOT, origins)
        return YamlMerged(node, origins)
    }

    private fun built(traced: Traced, pointer: JsonPointer, origins: MutableMap<JsonPointer, String>): YamlNode {
        count.checkDepth(pointer.tokens.size)
        origins[pointer] = traced.origin
        return when (traced) {
            is Traced.Whole -> traced.node

            is Traced.Members -> GraphNodes.withEntries(
                traced.template,
                traced.entries.map { entry ->
                    val child = pointer.child(Members.tokenOf(entry.key))
                    YamlEntry(key = entry.key, value = built(entry.value, child, origins))
                },
                traced.template.anchor,
            )

            is Traced.Items -> GraphNodes.withItems(
                traced.template,
                traced.items.mapIndexed { index, item -> built(item, pointer.child(index), origins) },
                traced.template.anchor,
            )
        }
    }

    companion object {
        fun items(traced: Traced): Traced.Items? = when (traced) {
            is Traced.Items -> traced

            is Traced.Members -> null

            is Traced.Whole -> (traced.node as? YamlNode.Sequence)?.let { sequence ->
                Traced.Items(sequence, sequence.items.map { item -> Traced.Whole(item, traced.origin) }, traced.origin)
            }
        }
    }
}
