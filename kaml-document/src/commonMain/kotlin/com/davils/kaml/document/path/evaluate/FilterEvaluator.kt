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

package com.davils.kaml.document.path.evaluate

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.path.filter.FilterExpression
import com.davils.kaml.document.path.filter.FilterOperand
import com.davils.kaml.document.path.filter.FunctionArgument
import com.davils.kaml.document.path.filter.FunctionCall
import com.davils.kaml.document.path.function.ArgumentValue
import com.davils.kaml.document.path.function.FunctionContext

internal class FilterEvaluator(
    private val paths: PathEvaluator,
    private val functions: FunctionContext,
    private val comparison: JsonComparison,
) {
    fun test(expression: FilterExpression, current: YamlNode): Boolean {
        functions.budget.charge()
        return tested(expression, current)
    }

    private fun tested(expression: FilterExpression, current: YamlNode): Boolean = when (expression) {
        is FilterExpression.Or -> expression.operands.any { test(it, current) }

        is FilterExpression.And -> expression.operands.all { test(it, current) }

        is FilterExpression.Not -> !test(expression.operand, current)

        is FilterExpression.Comparison -> comparison.compare(
            left = value(expression.left, current),
            operator = expression.operator,
            right = value(expression.right, current),
        )

        is FilterExpression.Existence -> paths.evaluate(expression.query, current).isNotEmpty()

        is FilterExpression.Call -> (called(expression.call, current) as ArgumentValue.Logical).isTrue
    }

    private fun value(operand: FilterOperand, current: YamlNode): YamlNode? {
        functions.budget.charge()
        return valueOf(operand, current)
    }

    private fun valueOf(operand: FilterOperand, current: YamlNode): YamlNode? = when (operand) {
        is FilterOperand.Literal -> operand.value
        is FilterOperand.Query -> paths.evaluate(operand.query, current).firstOrNull()?.node
        is FilterOperand.Call -> (called(operand.call, current) as ArgumentValue.Value).node
    }

    private fun called(call: FunctionCall, current: YamlNode): ArgumentValue {
        val arguments = call.arguments.map { argument -> argumentValue(argument, current) }
        return call.function.evaluate(arguments, functions)
    }

    private fun argumentValue(argument: FunctionArgument, current: YamlNode): ArgumentValue = when (argument) {
        is FunctionArgument.Value -> ArgumentValue.Value(value(argument.operand, current))
        is FunctionArgument.Nodes -> ArgumentValue.Nodes(paths.evaluate(argument.query, current).map { it.node })
    }
}
