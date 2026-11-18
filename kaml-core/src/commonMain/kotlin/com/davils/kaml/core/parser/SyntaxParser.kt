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

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.limit.YamlLimits

internal object SyntaxParser {
    fun parse(
        text: String,
        maximumDepth: Int = YamlLimits.HARD_DEPTH,
        maximumDiagnostics: Int = YamlLimits.HARD_DIAGNOSTICS,
        maximumNodes: Long = YamlLimits.HARD_NODES,
    ): ParsedSyntax {
        val state = ParseState(
            source = text,
            maximumDepth = maximumDepth,
            maximumDiagnostics = maximumDiagnostics,
            maximumNodes = maximumNodes,
        )
        val trivia = Trivia(state)
        val properties = Properties(state, trivia)
        val plain = PlainScalars(state, trivia)
        val quoted = QuotedScalars(state)
        val flow = FlowNodes(state = state, trivia = trivia, properties = properties, plain = plain, quoted = quoted)
        val blocks = BlockNodes(
            state = state,
            trivia = trivia,
            properties = properties,
            flow = flow,
            blockScalars = BlockScalars(state, trivia),
        )
        Documents(state, trivia, blocks).stream()
        reportInvalidCharacters(state)
        return ParsedSyntax(root = state.builder.finish(), diagnostics = state.diagnostics.sortedBy { it.range })
    }

    private fun reportInvalidCharacters(state: ParseState) {
        val text = state.text
        var index = 0
        while (index < text.length) {
            val printable = text.printableLengthAt(index)
            if (printable == 0) index = reportRun(state, index)
            index += printable
        }
    }

    private fun reportRun(state: ParseState, start: Int): Int {
        var end = start + 1
        while (end < state.text.length && state.text.printableLengthAt(end) == 0) {
            end++
        }
        val range = state.span(start = start, end = end)
        state.report(YamlDiagnosticCode.InvalidCharacter, range, "YAML does not allow these characters")
        return end
    }
}
