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

import com.davils.kaml.document.path.filter.FilterExpression
import com.davils.kaml.document.path.filter.FilterOperand
import com.davils.kaml.document.path.filter.FunctionArgument
import com.davils.kaml.document.path.filter.FunctionCall
import com.davils.kaml.document.path.function.ParameterType
import com.davils.kaml.document.path.function.ResultType
import com.davils.kaml.document.path.query.PathQuery

internal class FilterTyping(private val cursor: PathCursor) {
    fun test(primary: Primary): FilterExpression = when (primary) {
        is Primary.Literal -> cursor.fail("a literal is not a test")
        is Primary.Query -> FilterExpression.Existence(primary.query)
        is Primary.Call -> logicalCall(primary.call)
    }

    fun comparable(primary: Primary): FilterOperand = when (primary) {
        is Primary.Literal -> FilterOperand.Literal(primary.value)
        is Primary.Query -> singularQuery(primary.query)
        is Primary.Call -> valueCall(primary.call)
    }

    fun argument(primary: Primary, type: ParameterType): FunctionArgument = when (type) {
        ParameterType.Value -> FunctionArgument.Value(comparable(primary))
        ParameterType.Nodes -> nodes(primary)
    }

    private fun logicalCall(call: FunctionCall): FilterExpression {
        if (call.function.result != ResultType.Logical) cursor.fail("${call.function.name}() is not a test")
        return FilterExpression.Call(call)
    }

    private fun singularQuery(query: PathQuery): FilterOperand {
        if (!query.isSingular) cursor.fail("only a singular query can be compared")
        return FilterOperand.Query(query)
    }

    private fun valueCall(call: FunctionCall): FilterOperand {
        if (call.function.result != ResultType.Value) cursor.fail("${call.function.name}() does not return a value")
        return FilterOperand.Call(call)
    }

    private fun nodes(primary: Primary): FunctionArgument {
        val query = primary as? Primary.Query ?: cursor.fail("expected a query as argument")
        return FunctionArgument.Nodes(query.query)
    }
}
