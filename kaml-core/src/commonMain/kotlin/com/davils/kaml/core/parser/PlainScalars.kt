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

internal class PlainScalars(private val state: ParseState, private val trivia: Trivia) {
    private val cursor = state.cursor

    fun isPlainStartAt(index: Int, context: FlowContext): Boolean {
        val character = state.charAt(index)
        if (!character.isIndicator()) return character.isContent()
        val isLead = character == '-' || character == '?' || character == ':'
        return isLead && state.charAt(index + 1).isPlainSafe(context.isInsideFlow)
    }

    fun plain(indent: Int, context: FlowContext) {
        state.emitTo(YamlSyntaxKind.PLAIN_SCALAR, plainEnd(state.position, indent, context))
    }

    fun plainEnd(start: Int, indent: Int, context: FlowContext): Int {
        var end = lineContentEnd(start, context)
        var next = continuation(end, indent, context)
        while (next >= 0) {
            end = lineContentEnd(next, context)
            next = continuation(end, indent, context)
        }
        return end
    }

    private fun lineContentEnd(start: Int, context: FlowContext): Int {
        var index = start
        var end = start
        while (continuesAt(index, start, context)) {
            val character = state.charAt(index)
            index++
            if (!character.isWhite()) end = index
        }
        return end
    }

    private fun continuesAt(index: Int, start: Int, context: FlowContext): Boolean {
        val character = state.charAt(index)
        val isValueIndicator = character == ':' && !state.charAt(index + 1).isPlainSafe(context.isInsideFlow)
        val isComment = character == '#' && index > start && state.charAt(index - 1).isWhite()
        val isFlowEnd = context.isInsideFlow && character.isFlowIndicator()
        val isMark = character == BYTE_ORDER_MARK
        val isStop = character.isBreak() || character == END || isValueIndicator || isComment || isFlowEnd || isMark
        return !isStop
    }

    private fun continuation(end: Int, indent: Int, context: FlowContext): Int {
        val afterSpace = trivia.whitespaceEnd(end)
        val isAtBreak = state.charAt(afterSpace).isBreak()
        if (!context.isMultiLine || !isAtBreak) return -1
        return nextLineContent(cursor.nextLineStart(afterSpace), indent, context)
    }

    private fun nextLineContent(from: Int, indent: Int, context: FlowContext): Int {
        var lineStart = from
        var content = trivia.whitespaceEnd(lineStart)
        while (state.charAt(content).isBreak() && !cursor.isDocumentMarkerAt(lineStart)) {
            lineStart = cursor.nextLineStart(content)
            content = trivia.whitespaceEnd(lineStart)
        }
        val spaces = spacesFrom(lineStart)
        val isAccepted = !cursor.isDocumentMarkerAt(lineStart) &&
            spaces >= indent &&
            state.charAt(content) != '#' &&
            continuesAt(content, content, context)
        return content.takeIf { isAccepted } ?: -1
    }

    private fun spacesFrom(lineStart: Int): Int {
        var index = lineStart
        while (state.charAt(index) == SPACE) {
            index++
        }
        return index - lineStart
    }
}
