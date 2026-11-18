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

package com.davils.kaml.document.path.function

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.path.regexp.IRegexp

internal class FunctionContext(private val limits: YamlLimits, val budget: WorkBudget) {
    private val compiled = HashMap<String, IRegexp>()
    private val refused = HashSet<String>()

    fun regexpOf(pattern: String): IRegexp? {
        if (pattern in refused) return null
        return compiled[pattern] ?: compiledNew(pattern)
    }

    private fun compiledNew(pattern: String): IRegexp? {
        budget.charge(pattern.length.toLong() + 1L)
        val regexp = IRegexp.compile(pattern, limits, budget)
        if (regexp == null) {
            refused.add(pattern)
            return null
        }
        budget.charge(regexp.size.toLong())
        compiled[pattern] = regexp
        return regexp
    }
}
