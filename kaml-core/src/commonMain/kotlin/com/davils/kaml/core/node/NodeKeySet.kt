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

internal class NodeKeySet {
    private val runs = ArrayList<List<YamlNode>>()

    fun add(key: YamlNode): Boolean {
        if (contains(key)) return false
        var carry = listOf(key)
        while (runs.isNotEmpty() && runs.last().size == carry.size) {
            carry = merged(first = runs.removeAt(runs.lastIndex), second = carry)
        }
        runs.add(carry)
        return true
    }

    private fun contains(key: YamlNode): Boolean = runs.any { run -> isIn(run, key) }

    private fun isIn(run: List<YamlNode>, key: YamlNode): Boolean =
        UniqueKeys.rankIn(size = run.size, keyOfRank = run::get, key = key) >= 0

    private fun merged(first: List<YamlNode>, second: List<YamlNode>): List<YamlNode> {
        val result = ArrayList<YamlNode>(first.size + second.size)
        var left = 0
        var right = 0
        while (left < first.size || right < second.size) {
            val isBoth = left < first.size && right < second.size
            val isLeftBefore = isBoth && isBefore(first = first[left], second = second[right])
            val isLeft = right == second.size || isLeftBefore
            if (isLeft) result.add(first[left++])
            if (!isLeft) result.add(second[right++])
        }
        return result
    }

    private fun isBefore(first: YamlNode, second: YamlNode): Boolean =
        NodeOrder.compare(first = first, second = second) <= 0
}
