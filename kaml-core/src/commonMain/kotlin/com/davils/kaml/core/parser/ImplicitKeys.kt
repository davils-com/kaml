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

package com.davils.kaml.core.parser

internal class ImplicitKeys(
    private val state: ParseState,
    private val trivia: Trivia,
    private val properties: Properties,
    private val flow: FlowNodes,
) {
    private val cursor = state.cursor

    fun isImplicitKeyAhead(): Boolean {
        val snapshot = state.snapshot()
        val start = state.position
        val line = cursor.line
        val column = cursor.column
        state.descend { implicitKey(column) }
        val end = state.position
        val afterSpace = trivia.whitespaceEnd(end)
        val isIndicator = state.charAt(afterSpace) == ':' &&
            (state.charAt(afterSpace + 1).isBlankOrEnd() || isJsonLike(start))
        val isKey = cursor.line == line && end - start <= MAXIMUM_KEY_LENGTH && isIndicator
        state.restore(snapshot)
        return isKey
    }

    suspend fun Descent.implicitKey(column: Int) {
        val checkpoint = state.builder.checkpoint()
        val hasProperties = properties.read()
        val isEmptyKey = state.charAt(trivia.whitespaceEnd(state.position)) == ':'
        if (hasProperties && !isEmptyKey) trivia.whitespace()
        with(flow) {
            contentInto(
                checkpoint = checkpoint,
                indent = column + 1,
                context = FlowContext.BLOCK_KEY,
                hasProperties = hasProperties,
            )
        }
    }

    private fun isJsonLike(start: Int): Boolean {
        var index = start
        while (properties.isPropertyAt(index)) {
            index = trivia.whitespaceEnd(propertyEnd(index))
        }
        return state.charAt(index) in JSON_STARTS
    }

    private fun propertyEnd(start: Int): Int {
        var index = start
        while (!state.charAt(index).isBlankOrEnd()) {
            index++
        }
        return index
    }

    private companion object {
        const val MAXIMUM_KEY_LENGTH: Int = 1024
        const val JSON_STARTS: String = "\"'[{"
    }
}
