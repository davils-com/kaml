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

package com.davils.kaml.document.internal

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget
import com.davils.kore.core.collection.OrderedIndex

internal class KeyedSlots<T : Any>(
    items: List<T>,
    private val budget: WorkBudget,
    private val keyOf: (T) -> String?,
    private val nodeOf: (T) -> YamlNode? = { null },
) {
    private val slots = ArrayList<T?>(items)
    private val keys = OrderedIndex<String>()
    private val positions = ArrayList<ArrayDeque<Int>>()
    private val hashes = OrderedIndex<Int>()
    private val buckets = ArrayList<ArrayDeque<Int>>()

    init {
        items.forEachIndexed { index, item -> indexed(item, index) }
    }

    val items: List<T> get() = slots.filterNotNull()

    fun positionOf(key: String, node: YamlNode? = null): Int? = livePositionOf(key) ?: node?.let(::equalPositionOf)

    fun at(position: Int): T? = slots[position]

    fun remove(position: Int?) {
        val removed = position ?: return
        slots[removed] = null
    }

    fun append(item: T) {
        indexed(item, slots.size)
        slots.add(item)
    }

    fun replace(position: Int, item: T) {
        slots[position] = item
    }

    private fun indexed(item: T, position: Int) {
        keyOf(item)?.let { key -> groupOf(keys, positions, key).addLast(position) }
        nodeOf(item)?.let { node -> groupOf(hashes, buckets, node.hashCode()).addLast(position) }
    }

    private fun livePositionOf(key: String): Int? {
        val group = keys.ordinalOf(key)?.let(positions::get)
        return group?.firstOrNull { position -> slots[position] != null }
    }

    private fun equalPositionOf(node: YamlNode): Int? {
        val isText = node is YamlNode.Scalar && node.tag == YamlTag.STRING
        val bucket = hashes.ordinalOf(node.hashCode())?.let(buckets::get)
        return bucket?.firstOrNull { position -> !isText && isEqualAt(position, node) }
    }

    private fun isEqualAt(position: Int, node: YamlNode): Boolean {
        budget.chargeOrFail()
        val item = slots[position]
        return nodeOf(item ?: return false) == node
    }

    private fun <K : Comparable<K>> groupOf(
        index: OrderedIndex<K>,
        groups: MutableList<ArrayDeque<Int>>,
        key: K,
    ): ArrayDeque<Int> {
        val ordinal = index.ordinalFor(key)
        if (ordinal == groups.size) groups.add(ArrayDeque())
        return groups[ordinal]
    }
}
