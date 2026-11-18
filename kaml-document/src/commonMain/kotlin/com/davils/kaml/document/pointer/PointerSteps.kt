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

package com.davils.kaml.document.pointer

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.internal.Members
import com.davils.kaml.document.internal.WorkBudget

internal object PointerSteps {
    fun child(node: YamlNode, token: String): YamlNode? = when (node) {
        is YamlNode.Scalar -> null
        is YamlNode.Sequence -> PointerTokens.indexOf(token)?.let(node.items::getOrNull)
        is YamlNode.Mapping -> Members.first(node, token)?.value
    }

    fun child(node: YamlNode, token: String, budget: WorkBudget): YamlNode? {
        budget.charge(lookupCost(node))
        return child(node, token)
    }

    fun resolved(root: YamlNode, pointer: JsonPointer, budget: WorkBudget): YamlNode? {
        var current: YamlNode? = root
        pointer.tokens.forEach { token -> current = current?.let { node -> child(node, token, budget) } }
        return current
    }

    private fun lookupCost(node: YamlNode): Long {
        val mapping = node as? YamlNode.Mapping ?: return 1L
        return mapping.entries.size + 1L
    }
}
