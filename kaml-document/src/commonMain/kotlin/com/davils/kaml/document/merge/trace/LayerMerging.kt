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

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.internal.KeyedSlots
import com.davils.kaml.document.internal.Members
import com.davils.kaml.document.internal.NodeCount
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.merge.YamlLayer
import com.davils.kaml.document.merge.YamlMergeOptions
import com.davils.kaml.document.merge.YamlMerged
import com.davils.kaml.document.merge.YamlSequenceMerge
import com.davils.kaml.document.pointer.JsonPointer

private const val ANY: String = "*"

internal class LayerMerging(private val options: YamlMergeOptions, limits: YamlLimits) {
    private val budget = WorkBudget(limits.maximumNodes, "maximumNodes")
    private val count = NodeCount(budget, limits.maximumDepth)
    private val markers = DeletionMarkers(options.deletion, count)
    private val keyed = KeyedItems(this, markers, budget)

    fun merged(layers: List<YamlLayer>): YamlMerged {
        val first = layers.first()
        val initial: Traced = whole(first.node, first.name, 0)
        val overlays = layers.drop(1)
        val traced = overlays.fold(initial) { base, layer ->
            merged(base, Overlay(layer.node, layer.name, JsonPointer.ROOT))
        }
        return TracedNodes(count).merged(traced)
    }

    fun merged(base: Traced, overlay: Overlay): Traced {
        count.checkDepth(overlay.pattern.tokens.size)
        return when (val node = overlay.node) {
            is YamlNode.Scalar -> whole(node, overlay.layer, overlay.pattern.tokens.size)
            is YamlNode.Mapping -> mergedMapping(base, node, overlay)
            is YamlNode.Sequence -> mergedSequence(base, node, overlay)
        }
    }

    fun whole(node: YamlNode, layer: String, depth: Int): Traced = Traced.Whole(markers.stripped(node, depth), layer)

    private fun mergedMapping(base: Traced, mapping: YamlNode.Mapping, overlay: Overlay): Traced {
        val members = TracedNodes.members(base) ?: return whole(mapping, overlay.layer, overlay.pattern.tokens.size)
        val slots = KeyedSlots(
            items = members.entries,
            budget = budget,
            keyOf = { entry -> Members.tokenOf(entry.key) },
            nodeOf = TracedEntry::key,
        )
        mapping.entries.forEach { entry -> withEntry(slots, entry, overlay) }
        return Traced.Members(members.template, slots.items, overlay.layer)
    }

    private fun mergedSequence(base: Traced, sequence: YamlNode.Sequence, overlay: Overlay): Traced {
        val items = TracedNodes.items(base) ?: return whole(sequence, overlay.layer, overlay.pattern.tokens.size)
        return mergedItems(items, sequence, overlay)
    }

    private fun withEntry(slots: KeyedSlots<TracedEntry>, entry: YamlEntry, overlay: Overlay) {
        val token = Members.tokenOf(entry.key)
        val position = slots.positionOf(token, entry.key)
        if (markers.isDeletion(entry.value)) return slots.remove(position)
        val existing = position?.let(slots::at)
        val child = overlay.child(entry.value, token)
        val replacement = TracedEntry(existing?.key ?: entry.key, mergedOrWhole(existing?.value, child))
        if (position == null) return slots.append(replacement)
        slots.replace(position, replacement)
    }

    private fun mergedOrWhole(existing: Traced?, overlay: Overlay): Traced {
        if (existing == null) return whole(overlay.node, overlay.layer, overlay.pattern.tokens.size)
        return merged(existing, overlay)
    }

    private fun mergedItems(base: Traced.Items, sequence: YamlNode.Sequence, overlay: Overlay): Traced =
        when (val merge = options.sequenceMergeAt(overlay.pattern)) {
            YamlSequenceMerge.Replace -> whole(sequence, overlay.layer, overlay.pattern.tokens.size)

            YamlSequenceMerge.Append -> Traced.Items(
                template = base.template,
                items = base.items + appended(sequence, overlay),
                origin = overlay.layer,
            )

            is YamlSequenceMerge.ByKey -> keyed.merged(
                base = base,
                sequence = sequence,
                items = overlay.child(sequence, ANY),
                key = merge.key,
            )
        }

    private fun appended(sequence: YamlNode.Sequence, overlay: Overlay): List<Traced> {
        val kept = sequence.items.filterNot(markers::isDeletion)
        return kept.map { item -> whole(item, overlay.layer, overlay.pattern.tokens.size + 1) }
    }
}
