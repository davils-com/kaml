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

internal object UniqueKeys {
    private const val NONE: Int = -1

    fun indexOf(entries: List<YamlEntry>): KeyIndex {
        val keys = entries.map(YamlEntry::key)
        val sorted = sortedPositions(keys)
        val firsts = firstPositions(keys, sorted)
        val kept = IntArray(entries.size)
        val unique = buildList {
            entries.forEachIndexed { position, entry ->
                kept[position] = size
                if (firsts[position] == position) add(entry)
            }
        }
        val order = sorted.filter { position -> firsts[position] == position }
        return KeyIndex(unique, IntArray(order.size) { rank -> kept[order[rank]] })
    }

    fun requireUnique(entries: List<YamlEntry>) {
        val keys = entries.map(YamlEntry::key)
        val firsts = firstPositions(keys, sortedPositions(keys))
        val repeated = firsts.indices.firstOrNull { position -> firsts[position] != position } ?: NONE
        require(repeated == NONE) { "A mapping holds every key once, but entry $repeated repeats an earlier key" }
    }

    fun firstPositions(keys: List<YamlNode>): IntArray = firstPositions(keys, sortedPositions(keys))

    fun rankIn(size: Int, keyOfRank: (Int) -> YamlNode, key: YamlNode): Int {
        var low = 0
        var high = size - 1
        var found = -1
        while (found < 0 && low <= high) {
            val middle = low + high ushr 1
            val order = NodeOrder.compare(first = keyOfRank(middle), second = key)
            if (order == 0) found = middle
            if (order < 0) low = middle + 1
            if (order > 0) high = middle - 1
        }
        return found
    }

    private fun sortedPositions(keys: List<YamlNode>): List<Int> = keys.indices.sortedWith { left, right ->
        val order = NodeOrder.compare(first = keys[left], second = keys[right])
        order.takeIf { it != 0 } ?: left.compareTo(right)
    }

    private fun firstPositions(keys: List<YamlNode>, sorted: List<Int>): IntArray {
        val firsts = IntArray(keys.size) { position -> position }
        for (rank in 1 until sorted.size) {
            val previous = sorted[rank - 1]
            val current = sorted[rank]
            val isRepeated = NodeOrder.compare(first = keys[previous], second = keys[current]) == 0
            if (isRepeated) firsts[current] = firsts[previous]
        }
        return firsts
    }
}
