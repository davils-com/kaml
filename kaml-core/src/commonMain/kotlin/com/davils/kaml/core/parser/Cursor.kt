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

internal class Cursor(val text: String) {
    var position: Int = 0
        private set

    var lineStart: Int = 0
        private set

    var line: Int = 0
        private set

    private var knownFrom: Int = -1

    private var knownEnd: Int = -1

    val column: Int get() = position - lineStart

    val isFreshLine: Boolean get() = position == lineStart

    fun peek(ahead: Int = 0): Char = charAt(position + ahead)

    fun charAt(index: Int): Char = text.getOrElse(index) { END }

    fun advance(count: Int) {
        repeat(count) { step() }
    }

    fun advanceTo(target: Int) {
        advance(target - position)
    }

    fun mark(): CursorMark = CursorMark(position = position, lineStart = lineStart, line = line)

    fun restore(mark: CursorMark) {
        position = mark.position
        lineStart = mark.lineStart
        line = mark.line
    }

    fun isLineStartAt(index: Int): Boolean = index == 0 || charAt(index - 1).isBreak()

    fun lineEndOf(index: Int): Int {
        val isKnown = index in knownFrom..knownEnd
        if (!isKnown) remember(index)
        return knownEnd
    }

    fun nextLineStart(index: Int): Int {
        val end = lineEndOf(index)
        val isPair = charAt(end) == CARRIAGE_RETURN && charAt(end + 1) == LINE_FEED
        if (isPair) return end + 2
        return minOf(a = end + 1, b = text.length)
    }

    fun isDocumentMarkerAt(index: Int): Boolean {
        val isLineStart = index == 0 || charAt(index - 1).isBreak() || charAt(index - 1) == BYTE_ORDER_MARK
        val isMarker = text.startsWith(DOCUMENT_START, index) || text.startsWith(DOCUMENT_END, index)
        return isLineStart && isMarker && charAt(index + MARKER_LENGTH).isBlankOrEnd()
    }

    private fun remember(index: Int) {
        var end = index
        while (end < text.length && end != knownFrom && !text[end].isBreak()) {
            end++
        }
        val isJoined = end == knownFrom
        if (!isJoined) knownEnd = end
        knownFrom = index
    }

    private fun step() {
        val character = text[position]
        position++
        val isLoneReturn = character == CARRIAGE_RETURN && peek() != LINE_FEED
        val isLineEnd = character == LINE_FEED || isLoneReturn
        if (character == BYTE_ORDER_MARK && position - 1 == lineStart) lineStart = position
        if (isLineEnd) {
            lineStart = position
            line++
        }
    }

    companion object {
        const val DOCUMENT_START: String = "---"
        const val DOCUMENT_END: String = "..."
        const val MARKER_LENGTH: Int = 3
    }
}
