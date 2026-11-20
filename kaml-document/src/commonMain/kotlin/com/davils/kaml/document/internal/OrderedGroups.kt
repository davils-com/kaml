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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedIndex

internal class OrderedGroups<K : Comparable<K>, V> {
    private val index = OrderedIndex<K>()
    private val keyList = ArrayList<K>()
    private val groups = ArrayList<MutableList<V>>()

    val size: Int get() = groups.size

    val keys: List<K> get() = keyList

    val values: List<List<V>> get() = groups

    fun add(key: K, value: V) {
        val ordinal = index.ordinalFor(key)
        if (ordinal == groups.size) {
            keyList.add(key)
            groups.add(ArrayList())
        }
        groups[ordinal].add(value)
    }

    operator fun get(key: K): List<V>? = index.ordinalOf(key)?.let(groups::get)

    fun firstOf(key: K?): V? {
        val ordinal = key?.let(index::ordinalOf) ?: return null
        return groups[ordinal].first()
    }

    operator fun contains(key: K): Boolean = index.ordinalOf(key) != null

    fun all(predicate: (K, List<V>) -> Boolean): Boolean =
        keyList.indices.all { at -> predicate(keyList[at], groups[at]) }

    fun forEach(action: (K, List<V>) -> Unit) {
        keyList.forEachIndexed { at, key -> action(key, groups[at]) }
    }
}
