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

import com.davils.kaml.document.path.filter.FunctionArgument
import com.davils.kaml.document.path.filter.FunctionCall
import com.davils.kaml.document.path.function.ParameterType
import com.davils.kaml.document.path.function.PathFunction

internal class FunctionParser(
    private val cursor: PathCursor,
    private val filters: FilterParser,
    private val typing: FilterTyping,
) {
    fun call(): FunctionCall {
        if (cursor.current?.let(NameCharacters::isFunctionFirst) != true) cursor.fail("expected a filter expression")
        val name = cursor.takeWhile(NameCharacters::isFunction)
        val function = PathFunction.named(name) ?: cursor.fail("the function is not known")
        return cursor.nested { called(function) }
    }

    private fun called(function: PathFunction): FunctionCall {
        cursor.expect("(")
        val arguments = function.parameters.mapIndexed { position, type -> spacedArgument(position, type) }
        cursor.skipBlanks()
        cursor.expect(")")
        return FunctionCall(function, arguments)
    }

    private fun spacedArgument(position: Int, type: ParameterType): FunctionArgument {
        cursor.skipBlanks()
        if (position > 0) cursor.expect(",")
        cursor.skipBlanks()
        return argument(type)
    }

    private fun argument(type: ParameterType): FunctionArgument {
        val isLogical = cursor.current == '!' || cursor.current == '('
        if (isLogical) cursor.fail(LOGICAL_ARGUMENT)
        val primary = filters.primary()
        if (filters.continuesLogically()) cursor.fail(LOGICAL_ARGUMENT)
        return typing.argument(primary, type)
    }

    private companion object {
        const val LOGICAL_ARGUMENT: String = "a logical expression is not a function argument"
    }
}
