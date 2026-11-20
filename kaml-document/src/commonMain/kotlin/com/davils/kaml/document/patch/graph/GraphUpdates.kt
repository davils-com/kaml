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

package com.davils.kaml.document.patch.graph

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.internal.chargeOrFail
import com.davils.kaml.document.patch.JsonPatchRefusal
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kaml.document.pointer.PointerSteps
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget

internal class GraphUpdates(private val budget: WorkBudget) {
    fun at(root: YamlNode, pointer: JsonPointer, update: (YamlNode) -> GraphStep): GraphStep {
        val ancestors = ArrayList<YamlNode>()
        var current: YamlNode? = root
        var index = 0
        while (current != null && index < pointer.tokens.size) {
            ancestors.add(current)
            budget.chargeOrFail(GraphNodes.widthOf(current))
            current = PointerSteps.child(current, pointer.tokens[index])
            index++
        }
        val target = current ?: return GraphStep.Refused(JsonPatchRefusal.MissingParent)
        budget.chargeOrFail(GraphNodes.widthOf(target))
        return update(target).map { changed -> rebuilt(ancestors, pointer.tokens, changed) }
    }

    private fun rebuilt(ancestors: List<YamlNode>, tokens: List<String>, changed: YamlNode): YamlNode {
        var node = changed
        for (index in ancestors.indices.reversed()) node = GraphNodes.withChild(ancestors[index], tokens[index], node)
        return node
    }
}
