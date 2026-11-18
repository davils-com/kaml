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

internal class KeyIndex(val entries: List<YamlEntry>, private val order: IntArray) {
    val size: Int get() = order.size

    fun entryOfRank(rank: Int): YamlEntry = entries[order[rank]]

    fun positionOf(key: YamlNode): Int {
        val rank = UniqueKeys.rankIn(size = size, keyOfRank = { rank -> entryOfRank(rank).key }, key = key)
        return order.getOrNull(rank) ?: -1
    }
}
