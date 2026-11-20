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

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget

internal class NodeCount(private val budget: WorkBudget, private val maximumDepth: Int) {
    fun charge(node: YamlNode, depth: Int) {
        val pending = ArrayDeque<Pending>()
        pending.addLast(Pending(node = node, depth = depth))
        while (pending.isNotEmpty()) {
            val current = pending.removeLast()
            budget.chargeOrFail()
            if (current.depth > maximumDepth) throw YamlDocumentLimitException(DEPTH)
            val childDepth = current.depth + 1
            childrenOf(current.node).forEach { child -> pending.addLast(Pending(node = child, depth = childDepth)) }
        }
    }

    fun checkDepth(depth: Int) {
        if (depth > maximumDepth) throw YamlDocumentLimitException(DEPTH)
    }

    private fun childrenOf(node: YamlNode): List<YamlNode> = when (node) {
        is YamlNode.Scalar -> emptyList()
        is YamlNode.Sequence -> node.items
        is YamlNode.Mapping -> node.entries.flatMap { entry -> listOf(entry.key, entry.value) }
    }

    private data class Pending(val node: YamlNode, val depth: Int)

    private companion object {
        const val DEPTH: String = "maximumDepth"
    }
}
