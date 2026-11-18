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

package com.davils.kaml.document.path.parse

import com.davils.kaml.document.path.filter.ComparisonOperator
import com.davils.kaml.document.path.filter.FilterExpression

private val logicalOperators: List<String> = ComparisonOperator.entries.map(ComparisonOperator::symbol) + "&&" + "||"

internal class FilterParser(
    private val cursor: PathCursor,
    private val paths: PathParser,
    private val literals: LiteralParser,
) {
    private val typing = FilterTyping(cursor)
    private val functions = FunctionParser(cursor, this, typing)

    fun logical(): FilterExpression {
        val operands = mutableListOf(conjunction())
        while (acceptOperator("||")) operands += conjunction()
        return operands.singleOrNull() ?: FilterExpression.Or(operands)
    }

    fun primary(): Primary {
        val literal = literals.literal()?.let(Primary::Literal)
        val query = literal ?: paths.filterQuery()?.let(Primary::Query)
        return query ?: Primary.Call(functions.call())
    }

    fun continuesLogically(): Boolean {
        val start = cursor.offset
        cursor.skipBlanks()
        val isOperator = logicalOperators.any { operator -> cursor.accept(operator) }
        cursor.reset(start)
        return isOperator
    }

    private fun conjunction(): FilterExpression {
        val operands = mutableListOf(basic())
        while (acceptOperator("&&")) operands += basic()
        return operands.singleOrNull() ?: FilterExpression.And(operands)
    }

    private fun basic(): FilterExpression {
        if (cursor.accept("!")) return negated()
        return positive()
    }

    private fun positive(): FilterExpression {
        if (cursor.current == '(') return parenthesized()
        return comparisonOrTest(primary())
    }

    private fun negated(): FilterExpression {
        cursor.skipBlanks()
        if (cursor.current == '(') return FilterExpression.Not(parenthesized())
        return FilterExpression.Not(typing.test(primary()))
    }

    private fun parenthesized(): FilterExpression = cursor.nested {
        cursor.expect("(")
        cursor.skipBlanks()
        val expression = logical()
        cursor.skipBlanks()
        cursor.expect(")")
        expression
    }

    private fun comparisonOrTest(left: Primary): FilterExpression {
        val operator = cursor.attempt(::comparisonOperator) ?: return typing.test(left)
        cursor.skipBlanks()
        val right = primary()
        return FilterExpression.Comparison(typing.comparable(left), operator, typing.comparable(right))
    }

    private fun comparisonOperator(): ComparisonOperator? {
        cursor.skipBlanks()
        return ComparisonOperator.entries.firstOrNull { operator -> cursor.accept(operator.symbol) }
    }

    private fun acceptOperator(operator: String): Boolean {
        val isFound = cursor.attempt {
            cursor.skipBlanks()
            operator.takeIf { cursor.accept(it) }
        } != null
        cursor.skipBlanks()
        return isFound
    }
}
