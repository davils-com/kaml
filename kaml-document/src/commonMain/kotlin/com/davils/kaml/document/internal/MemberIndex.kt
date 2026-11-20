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
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget
import com.davils.kore.core.collection.OrderedIndex

internal class MemberIndex(mapping: YamlNode.Mapping, budget: WorkBudget) {
    private val tokens = OrderedIndex<String>()
    private val values = ArrayList<YamlNode>()

    init {
        budget.chargeOrFail(mapping.entries.size.toLong())
        mapping.entries.forEach { entry ->
            val ordinal = tokens.ordinalFor(Members.tokenOf(entry.key))
            if (ordinal == values.size) values.add(entry.value)
        }
    }

    fun valueOf(token: String): YamlNode? = tokens.ordinalOf(token)?.let(values::get)
}
