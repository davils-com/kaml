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

@file:OptIn(DavilsInternal::class)

package com.davils.kaml.document.merge.trace

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.document.internal.KeyedSlots
import com.davils.kaml.document.internal.Members
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget

internal class KeyedItems(
    private val layers: LayerMerging,
    private val markers: DeletionMarkers,
    private val budget: WorkBudget,
) {
    fun merged(base: Traced.Items, sequence: YamlNode.Sequence, items: Overlay, key: String): Traced {
        val slots = base.slots.takeIf { base.key == key } ?: KeyedSlots(base.all, budget, keyOf = { keyOf(it, key) })
        sequence.items.forEach { item -> withItem(slots, items.with(item), key) }
        return Traced.Items(template = base.template, slots = slots, key = key, origin = items.layer)
    }

    private fun withItem(slots: KeyedSlots<Traced>, overlay: Overlay, key: String) {
        val identity = overlay.node[key].string()
        val position = identity?.let(slots::positionOf)
        if (markers.isDeletion(overlay.node)) return slots.remove(position)
        val existing = position?.let(slots::at) ?: return slots.append(whole(overlay))
        slots.replace(position, layers.merged(existing, overlay))
    }

    private fun whole(overlay: Overlay): Traced = layers.whole(overlay.node, overlay.layer, overlay.pattern.tokens.size)

    private fun keyOf(item: Traced, key: String): String? {
        val mapping = (item as? Traced.Whole)?.node as? YamlNode.Mapping ?: return null
        return Members.first(mapping, key)?.value.string()
    }
}
