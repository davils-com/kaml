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

package com.davils.kaml.document.path.regexp

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.document.internal.CodePoints
import com.davils.kaml.document.internal.WorkBudget

internal class IRegexp private constructor(private val program: List<Instruction>) {
    val size: Int get() = program.size

    fun matches(subject: String, budget: WorkBudget): Boolean? = run(subject, isWhole = true, budget = budget)

    fun containsMatchIn(subject: String, budget: WorkBudget): Boolean? = run(subject, isWhole = false, budget = budget)

    private fun run(subject: String, isWhole: Boolean, budget: WorkBudget): Boolean? {
        if (!budget.spend(subject.length.toLong())) return null
        return RegexpMachine(program).run(CodePoints.of(subject), isWhole, budget)
    }

    companion object {
        fun compile(pattern: String, limits: YamlLimits, budget: WorkBudget): IRegexp? {
            val admitted = pattern.takeIf { it.length <= limits.maximumRegexpLength }
            val parser = admitted?.let { text ->
                RegexpParser(text, maximumDepth = limits.maximumQueryNesting, maximumRepeat = limits.maximumRegexpSize)
            }
            val compiler = RegexpCompiler(limits.maximumRegexpSize, budget)
            val program = parser?.parse()?.let(compiler::compile)
            return program?.let(::IRegexp)
        }
    }
}
