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

package com.davils.kaml.serialization.decode

import com.davils.kaml.core.node.YamlNode
import kotlin.reflect.KClass

internal class DecodedKeys {
    private val keys = ArrayList<Key>()

    fun add(value: Any?, node: YamlNode) {
        keys.add(Key(value = value, node = node, position = keys.size, canonical = canonicalOf(value)))
    }

    fun repeated(): List<YamlNode> {
        val sorted = keys.sortedWith(order)
        val repeated = ArrayList<Key>()
        var start = 0
        while (start < sorted.size) {
            val hash = sorted[start].hash
            var end = start + 1
            while (end < sorted.size && sorted[end].hash == hash) end++
            repeated.addAll(repeatedIn(sorted.subList(fromIndex = start, toIndex = end)))
            start = end
        }
        val ordered = repeated.sortedBy(Key::position)
        return ordered.map(Key::node)
    }

    private fun repeatedIn(run: List<Key>): List<Key> {
        val distinct = ArrayList<Key>()
        val repeated = ArrayList<Key>()
        run.forEachIndexed { index, key ->
            val isRepeated = isCanonicalRepeat(run.getOrNull(index - 1), key) || isOtherRepeat(distinct, key)
            if (isRepeated) repeated.add(key)
            if (!isRepeated && key.canonical == null) distinct.add(key)
        }
        return repeated
    }

    private fun isCanonicalRepeat(previous: Key?, key: Key): Boolean {
        val canonical = key.canonical ?: return false
        return previous?.canonical == canonical && previous.value == key.value
    }

    private fun isOtherRepeat(distinct: List<Key>, key: Key): Boolean {
        if (key.canonical != null) return false
        return distinct.any { earlier -> earlier.value == key.value }
    }

    private data class Key(val value: Any?, val node: YamlNode, val position: Int, val canonical: String?) {
        val hash: Int = value.hashCode()
    }

    private companion object {
        const val NULL_TEXT: String = "null"

        private val byHash: Comparator<Key> = compareBy { key -> key.hash }
        private val byCanonical: Comparator<Key> = byHash.thenBy(nullsLast()) { key -> key.canonical }
        val order: Comparator<Key> = byCanonical.thenBy { key -> key.position }

        val canonicalTypes: Map<KClass<*>, String> = mapOf(
            String::class to "s",
            Boolean::class to "z",
            Char::class to "c",
            Byte::class to "b",
            Short::class to "h",
            Int::class to "i",
            Long::class to "l",
            UByte::class to "ub",
            UShort::class to "uh",
            UInt::class to "ui",
            ULong::class to "ul",
            Float::class to "f",
            Double::class to "d",
        )

        fun canonicalOf(value: Any?): String? {
            if (value == null) return NULL_TEXT
            val type = canonicalTypes[value::class]
            return type?.let { "$it:$value" }
        }
    }
}
