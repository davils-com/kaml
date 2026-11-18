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
import com.davils.kaml.core.syntax.YamlSyntaxKind

internal class BlockScalars(private val state: ParseState, private val trivia: Trivia) {
    private val cursor = state.cursor

    fun isBlockScalarAt(index: Int): Boolean {
        val character = state.charAt(index)
        return character == LITERAL || character == FOLDED
    }

    fun blockScalar(parentIndent: Int) {
        val indentation = header()
        trivia.whitespace()
        if (trivia.isCommentAt(state.position)) trivia.comment()
        state.unexpected(cursor.lineEndOf(state.position), "A block scalar header ends its line")
        if (state.peek() == END) return
        trivia.lineBreak()
        content(parentIndent, indentation)
    }

    private fun header(): Int {
        var index = state.position + 1
        var indentation = 0
        var chomping = NO_CHOMPING
        while (state.charAt(index) in HEADER_INDICATORS) {
            val character = state.charAt(index)
            val isDigit = character.isDigit()
            val isRepeatedDigit = isDigit && indentation > 0
            val isRepeatedChomping = !isDigit && chomping != NO_CHOMPING
            val isRepeated = isRepeatedDigit || isRepeatedChomping
            if (isRepeated || character == '0') {
                val range = state.span(start = index, end = index + 1)
                state.report(
                    YamlDiagnosticCode.InvalidBlockScalarHeader,
                    range,
                    "Repeated or zero block scalar indicator",
                )
            }
            if (isDigit) indentation = character.digitToInt()
            if (!isDigit) chomping = character
            index++
        }
        state.emitTo(YamlSyntaxKind.BLOCK_SCALAR_HEADER, index)
        return indentation
    }

    private fun content(parentIndent: Int, indentation: Int) {
        val start = state.position
        val indent = (maxOf(a = parentIndent, b = 0) + indentation).takeIf { indentation > 0 }
            ?: detectedIndent(start, parentIndent)
        val end = contentEnd(start, indent)
        state.emitTo(YamlSyntaxKind.BLOCK_SCALAR_CONTENT, end)
        checkTrailingLines(end)
    }

    private fun detectedIndent(start: Int, parentIndent: Int): Int {
        var lineStart = start
        var widestEmpty = 0
        var detected = -1
        while (detected < 0 && lineStart < state.text.length && !cursor.isDocumentMarkerAt(lineStart)) {
            val spaces = spacesFrom(lineStart)
            val isEmpty = isEmptyAfter(lineStart + spaces)
            if (!isEmpty) reportWideEmpty(widestEmpty, spaces, lineStart)
            if (!isEmpty) detected = maxOf(a = spaces, b = parentIndent + 1)
            widestEmpty = maxOf(a = widestEmpty, b = spaces)
            lineStart = cursor.nextLineStart(lineStart)
        }
        return detected.takeIf { it >= 0 } ?: maxOf(a = widestEmpty, b = parentIndent + 1)
    }

    private fun reportWideEmpty(widestEmpty: Int, spaces: Int, lineStart: Int) {
        if (widestEmpty <= spaces) return
        state.report(
            YamlDiagnosticCode.InvalidBlockScalarIndentation,
            state.span(start = lineStart, end = lineStart + spaces),
            "A leading empty line of a block scalar is indented more than its first line",
        )
    }

    private fun contentEnd(start: Int, indent: Int): Int {
        var lineStart = start
        var end = start
        while (lineStart < state.text.length && !cursor.isDocumentMarkerAt(lineStart) && belongs(lineStart, indent)) {
            lineStart = cursor.nextLineStart(lineStart)
            end = lineStart
        }
        return end
    }

    private fun belongs(lineStart: Int, indent: Int): Boolean {
        val spaces = spacesFrom(lineStart)
        return spaces >= indent || isEmptyAfter(lineStart + spaces)
    }

    private fun isEmptyAfter(index: Int): Boolean {
        val character = state.charAt(index)
        return character.isBreak() || character == END
    }

    private fun checkTrailingLines(from: Int) {
        var lineStart = from
        var isTrailing = true
        while (isTrailing && lineStart < state.text.length) {
            val content = trivia.whitespaceEnd(lineStart)
            val character = state.charAt(content)
            val isBlankOrComment = isEmptyAfter(content) || character == '#'
            val hasTab = (lineStart until content).any { index -> state.charAt(index) == TAB }
            if (isBlankOrComment && hasTab) {
                val range = state.span(start = lineStart, end = content)
                state.report(YamlDiagnosticCode.TabIndentation, range, "Tabs cannot follow a block scalar")
            }
            isTrailing = isBlankOrComment && character != '#'
            lineStart = cursor.nextLineStart(lineStart)
        }
    }

    private fun spacesFrom(lineStart: Int): Int {
        var index = lineStart
        while (state.charAt(index) == SPACE) {
            index++
        }
        return index - lineStart
    }

    private companion object {
        const val LITERAL: Char = '|'
        const val FOLDED: Char = '>'
        const val NO_CHOMPING: Char = ' '
        const val HEADER_INDICATORS: String = "0123456789+-"
    }
}
