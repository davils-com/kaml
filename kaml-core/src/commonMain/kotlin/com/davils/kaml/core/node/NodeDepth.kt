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

import com.davils.kaml.core.limit.YamlLimits

internal object NodeDepth {
    const val MAXIMUM: Int = YamlLimits.HARD_DEPTH

    fun ofItems(items: List<YamlNode>): Int {
        val deepest = items.maxOfOrNull(YamlNode::depth) ?: 0
        return checked(deepest + 1)
    }

    fun ofEntries(entries: List<YamlEntry>): Int {
        val deepest = entries.maxOfOrNull { entry -> maxOf(a = entry.key.depth, b = entry.value.depth) } ?: 0
        return checked(deepest + 1)
    }

    private fun checked(depth: Int): Int {
        require(depth <= MAXIMUM) { "A node cannot nest deeper than $MAXIMUM levels" }
        return depth
    }
}
