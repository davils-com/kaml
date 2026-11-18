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

package com.davils.kaml.document.patch.merge

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.isNull
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.document.internal.KeyedSlots
import com.davils.kaml.document.internal.Members
import com.davils.kaml.document.internal.NodeCount
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.patch.graph.GraphNodes

internal class MergePatching(limits: YamlLimits) {
    private val budget = WorkBudget(limits.maximumNodes, "maximumNodes")
    private val count = NodeCount(budget, limits.maximumDepth)

    fun merged(target: YamlNode?, patch: YamlNode): YamlNode = merged(target, patch, 0)

    private fun merged(target: YamlNode?, patch: YamlNode, depth: Int): YamlNode {
        val members = patch as? YamlNode.Mapping ?: return charged(patch, depth)
        count.checkDepth(depth)
        val base = target as? YamlNode.Mapping ?: yamlMapping(emptyList())
        val slots = KeyedSlots(
            items = base.entries,
            budget = budget,
            keyOf = { entry -> Members.tokenOf(entry.key) },
            nodeOf = YamlEntry::key,
        )
        members.entries.forEach { member -> withMember(slots, member, depth) }
        return GraphNodes.withEntries(base, slots.items, anchor = null)
    }

    private fun withMember(slots: KeyedSlots<YamlEntry>, member: YamlEntry, depth: Int) {
        count.charge(member.key, depth + 1)
        val token = Members.tokenOf(member.key)
        val position = slots.positionOf(token, member.key)
        if (member.value.isNull) return slots.remove(position)
        val existing = position?.let(slots::at)
        val value = merged(target = existing?.value, patch = member.value, depth = depth + 1)
        val entry = YamlEntry(key = existing?.key ?: member.key, value = value)
        if (position == null) return slots.append(entry)
        slots.replace(position, entry)
    }

    private fun charged(patch: YamlNode, depth: Int): YamlNode {
        count.charge(patch, depth)
        return patch
    }
}
