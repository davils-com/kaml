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

package com.davils.kaml.document.internal

internal class KeyedSlots<T : Any>(
    items: List<T>,
    private val keyOf: (T) -> String?,
    private val nodeOf: (T) -> Any? = { null },
) {
    private val slots = ArrayList<T?>(items)
    private val positions = HashMap<String, ArrayDeque<Int>>()
    private val nodes = HashMap<Any, Int>()

    init {
        items.forEachIndexed { index, item -> indexed(item, index) }
    }

    val items: List<T> get() = slots.filterNotNull()

    fun positionOf(key: String?, node: Any? = null): Int? {
        val byKey = key?.let(positions::get)?.firstOrNull()
        return byKey ?: node?.let(nodes::get)
    }

    fun at(position: Int): T? = slots[position]

    fun remove(position: Int?) {
        val item = position?.let(slots::get) ?: return
        slots[position] = null
        keyOf(item)?.let { key -> positions[key]?.remove(position) }
        nodeOf(item)?.let(nodes::remove)
    }

    fun append(item: T) {
        indexed(item, slots.size)
        slots.add(item)
    }

    fun replace(position: Int, item: T) {
        slots[position] = item
    }

    private fun indexed(item: T, position: Int) {
        keyOf(item)?.let { key -> positions.getOrPut(key) { ArrayDeque() }.addLast(position) }
        nodeOf(item)?.let { node -> nodes.getOrPut(node) { position } }
    }
}
