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

internal class Trivia(private val state: ParseState) {
    private val cursor = state.cursor
    private var searchedFrom = -1
    private var found: ContentLine? = null

    fun whitespace() {
        state.emitTo(YamlSyntaxKind.WHITESPACE, whitespaceEnd(state.position))
    }

    fun whitespaceEnd(from: Int): Int {
        var end = from
        while (state.charAt(end).isWhite()) {
            end++
        }
        return end
    }

    fun isCommentAt(index: Int): Boolean {
        val isSeparated = cursor.isLineStartAt(index) || state.charAt(index - 1).isWhite()
        return state.charAt(index) == '#' && isSeparated
    }

    fun isLineEndAt(index: Int): Boolean {
        val after = whitespaceEnd(index)
        val character = state.charAt(after)
        return character.isBreak() || character == END || isCommentAt(after)
    }

    fun lineBreak() {
        val isPair = state.peek() == CARRIAGE_RETURN && state.peek(1) == LINE_FEED
        state.emit(YamlSyntaxKind.LINE_BREAK, BREAK_PAIR.takeIf { isPair } ?: 1)
    }

    fun comment() {
        val end = cursor.lineEndOf(state.position)
        reportMarks(state.position, end)
        state.emitTo(YamlSyntaxKind.COMMENT, end)
    }

    fun reportMarks(start: Int, end: Int) {
        var index = start
        while (index < end && state.charAt(index) != BYTE_ORDER_MARK) {
            index++
        }
        if (index < end) reportMark(index)
    }

    fun finishLine() {
        if (cursor.isFreshLine) return
        whitespace()
        if (isCommentAt(state.position)) comment()
        state.unexpected(cursor.lineEndOf(state.position), "Expected the end of the line")
    }

    fun nextContentLine(): ContentLine? = contentLineFrom(scanStart())

    fun contentLineFrom(from: Int): ContentLine? {
        if (from != searchedFrom) {
            found = searchFrom(from)
            searchedFrom = from
        }
        return found
    }

    private fun searchFrom(from: Int): ContentLine? {
        var lineStart = from
        while (lineStart < state.text.length) {
            val line = lineAt(lineStart)
            if (line != null) return line
            lineStart = cursor.nextLineStart(lineStart)
        }
        return null
    }

    fun consumeTo(target: Int, isStreamPrefix: Boolean = false) {
        while (state.position < target) {
            consumeOne(target, isStreamPrefix)
        }
    }

    private fun consumeOne(target: Int, isStreamPrefix: Boolean) {
        val isConsumed = breakConsumed() ||
            whitespaceConsumed(target) ||
            commentConsumed(target) ||
            markConsumed(isStreamPrefix)
        if (!isConsumed) state.unexpected(target, "Expected only blank lines and comments")
    }

    private fun breakConsumed(): Boolean {
        if (!state.peek().isBreak()) return false
        lineBreak()
        return true
    }

    private fun whitespaceConsumed(target: Int): Boolean {
        if (!state.peek().isWhite()) return false
        state.emitTo(YamlSyntaxKind.WHITESPACE, minOf(a = whitespaceEnd(state.position), b = target))
        return true
    }

    private fun commentConsumed(target: Int): Boolean {
        if (state.peek() != '#') return false
        val end = minOf(a = cursor.lineEndOf(state.position), b = target)
        reportMarks(state.position, end)
        state.emitTo(YamlSyntaxKind.COMMENT, end)
        return true
    }

    private fun markConsumed(isStreamPrefix: Boolean): Boolean {
        if (state.peek() != BYTE_ORDER_MARK) return false
        val isAllowed = isStreamPrefix || cursor.isDocumentMarkerAt(state.position + 1)
        if (!isAllowed) reportMark(state.position)
        state.emit(YamlSyntaxKind.BYTE_ORDER_MARK, 1)
        return true
    }

    private fun reportMark(index: Int) {
        val range = state.span(start = index, end = index + 1)
        state.report(YamlDiagnosticCode.UnexpectedContent, range, "A byte order mark can only start a document")
    }

    private fun scanStart(): Int {
        if (cursor.isFreshLine) return state.position
        return cursor.nextLineStart(state.position)
    }

    private fun lineAt(lineStart: Int): ContentLine? {
        val start = lineStart + (1.takeIf { state.charAt(lineStart) == BYTE_ORDER_MARK } ?: 0)
        var spacesEnd = start
        while (state.charAt(spacesEnd) == SPACE) {
            spacesEnd++
        }
        val contentStart = whitespaceEnd(spacesEnd)
        val character = state.charAt(contentStart)
        val isBlank = character.isBreak() || character == END || character == '#'
        if (isBlank) return null
        return ContentLine(
            lineStart = lineStart,
            contentStart = contentStart,
            indent = spacesEnd - start,
            hasTabIndentation = contentStart > spacesEnd,
            isDocumentMarker = cursor.isDocumentMarkerAt(start),
        )
    }

    fun reportTab(line: ContentLine) {
        state.report(
            YamlDiagnosticCode.TabIndentation,
            state.span(start = line.lineStart, end = line.contentStart),
            "Tabs cannot indent",
        )
    }

    private companion object {
        const val BREAK_PAIR: Int = 2
    }
}
