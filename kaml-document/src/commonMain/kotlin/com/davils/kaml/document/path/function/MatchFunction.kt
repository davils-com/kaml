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

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.path.regexp.IRegexp

internal sealed class MatchFunction(override val name: String) : PathFunction {
    override val parameters: List<ParameterType> = listOf(ParameterType.Value, ParameterType.Value)

    override val result: ResultType = ResultType.Logical

    override fun evaluate(arguments: List<ArgumentValue>, context: FunctionContext): ArgumentValue {
        val (subject, pattern) = arguments.map { argument -> textOf((argument as ArgumentValue.Value).node) }
        val regexp = subject?.let { pattern }?.let(context::regexpOf)
        val isMatch = subject != null && regexp != null && checked(regexp, subject, context.budget)
        return ArgumentValue.Logical(isMatch)
    }

    protected abstract fun matches(regexp: IRegexp, subject: String, budget: WorkBudget): Boolean?

    private fun checked(regexp: IRegexp, subject: String, budget: WorkBudget): Boolean =
        matches(regexp, subject, budget) ?: throw YamlDocumentLimitException(STEPS)

    private fun textOf(node: YamlNode?): String? {
        val scalar = node as? YamlNode.Scalar ?: return null
        return scalar.value.takeIf { scalar.tag == YamlTag.STRING }
    }

    object Whole : MatchFunction("match") {
        override fun matches(regexp: IRegexp, subject: String, budget: WorkBudget): Boolean? =
            regexp.matches(subject, budget)
    }

    object Partial : MatchFunction("search") {
        override fun matches(regexp: IRegexp, subject: String, budget: WorkBudget): Boolean? =
            regexp.containsMatchIn(subject, budget)
    }

    private companion object {
        const val STEPS: String = "maximumQuerySteps"
    }
}
