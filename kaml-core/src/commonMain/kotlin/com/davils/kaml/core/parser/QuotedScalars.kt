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

import com.davils.kaml.core.syntax.YamlSyntaxKind

internal class QuotedScalars(private val state: ParseState) {
    fun isQuoteAt(index: Int): Boolean {
        val character = state.charAt(index)
        return character == SINGLE_QUOTE || character == DOUBLE_QUOTE
    }

    fun quoted(indent: Int, context: FlowContext) {
        val quote = state.peek()
        val kind = YamlSyntaxKind.DOUBLE_QUOTED_SCALAR.takeIf { quote == DOUBLE_QUOTE }
            ?: YamlSyntaxKind.SINGLE_QUOTED_SCALAR
        val scan = QuotedScan(state, QuotedScan.Shape(quote = quote, indent = indent, context = context))
        state.emitTo(kind, scan.endFrom(state.position))
    }

    private companion object {
        const val SINGLE_QUOTE: Char = '\''
        const val DOUBLE_QUOTE: Char = '"'
    }
}
