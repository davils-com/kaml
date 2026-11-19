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

import com.davils.kaml.document.internal.OrderedIndex
import com.davils.kaml.document.pointer.JsonPointer

internal class OriginMap(origins: List<Pair<JsonPointer, String>>) : AbstractMap<JsonPointer, String>() {
    private val index = OrderedIndex<PointerKey>()
    private val pointers = ArrayList<JsonPointer>()
    private val names = ArrayList<String>()

    init {
        origins.forEach { (pointer, origin) ->
            val ordinal = index.ordinalFor(PointerKey(pointer.tokens))
            if (ordinal == pointers.size) {
                pointers.add(pointer)
                names.add(origin)
            }
            names[ordinal] = origin
        }
    }

    override val size: Int get() = pointers.size

    override val entries: Set<Map.Entry<JsonPointer, String>> = Entries()

    override fun get(key: JsonPointer): String? = index.ordinalOf(PointerKey(key.tokens))?.let(names::get)

    override fun containsKey(key: JsonPointer): Boolean = index.ordinalOf(PointerKey(key.tokens)) != null

    private inner class Entries : AbstractSet<Map.Entry<JsonPointer, String>>() {
        override val size: Int get() = pointers.size

        override fun iterator(): Iterator<Map.Entry<JsonPointer, String>> {
            val origins = pointers.mapIndexed { at, pointer -> Origin(pointer, names[at]) }
            return origins.iterator()
        }

        override fun contains(element: Map.Entry<JsonPointer, String>): Boolean = get(element.key) == element.value
    }

    private class Origin(override val key: JsonPointer, override val value: String) : Map.Entry<JsonPointer, String> {
        override fun equals(other: Any?): Boolean = other is Map.Entry<*, *> && key == other.key && value == other.value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()

        override fun toString(): String = "$key=$value"
    }

    private class PointerKey(private val tokens: List<String>) : Comparable<PointerKey> {
        override fun compareTo(other: PointerKey): Int {
            val shared = minOf(a = tokens.size, b = other.tokens.size)
            val differing = (0 until shared).firstOrNull { at -> tokens[at] != other.tokens[at] }
            val byToken = differing?.let { at -> tokens[at].compareTo(other.tokens[at]) }
            return byToken ?: tokens.size.compareTo(other.tokens.size)
        }
    }
}
