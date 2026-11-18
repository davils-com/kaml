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

package com.davils.kaml.serialization.json

import kotlinx.serialization.json.JsonElement

internal class JsonMembers(
    private val names: List<String>,
    private val elements: List<JsonElement>,
) : AbstractMap<String, JsonElement>() {
    private val order: IntArray = sortedPositions(names)

    override val entries: Set<Map.Entry<String, JsonElement>> = Entries(names, elements)

    override val size: Int get() = names.size

    override fun containsKey(key: String): Boolean = positionOf(key) >= 0

    override fun get(key: String): JsonElement? = elements.getOrNull(positionOf(key))

    private fun positionOf(name: String): Int {
        var low = 0
        var high = order.size - 1
        var found = NONE
        while (low <= high && found == NONE) {
            val middle = low + (high - low) / 2
            val comparison = names[order[middle]].compareTo(name)
            if (comparison == 0) found = order[middle]
            if (comparison < 0) low = middle + 1
            if (comparison > 0) high = middle - 1
        }
        return found
    }

    private class Entries(
        private val names: List<String>,
        private val elements: List<JsonElement>,
    ) : AbstractSet<Map.Entry<String, JsonElement>>() {
        override val size: Int get() = names.size

        override fun iterator(): Iterator<Map.Entry<String, JsonElement>> {
            val members = names.indices.asSequence().map { position -> Member(names[position], elements[position]) }
            return members.iterator()
        }
    }

    private class Member(override val key: String, override val value: JsonElement) : Map.Entry<String, JsonElement> {
        override fun equals(other: Any?): Boolean = other is Map.Entry<*, *> && key == other.key && value == other.value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()

        override fun toString(): String = "$key=$value"
    }

    companion object {
        private const val NONE: Int = -1

        fun firstRepeat(names: List<String?>): Int {
            val order = sortedPositions(names)
            var first = NONE
            for (index in 1 until order.size) {
                val position = order[index]
                val isRepeat = names[position] == names[order[index - 1]]
                if (isRepeat && (first == NONE || position < first)) first = position
            }
            return first
        }

        private fun sortedPositions(names: List<String?>): IntArray {
            val present = names.indices.filter { position -> names[position] != null }
            val sorted = present.sortedWith(compareBy<Int> { position -> names[position] }.thenBy { it })
            return sorted.toIntArray()
        }
    }
}
