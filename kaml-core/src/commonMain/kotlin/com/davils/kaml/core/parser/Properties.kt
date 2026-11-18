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
import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.syntax.YamlSyntaxKind

internal class Properties(private val state: ParseState, private val trivia: Trivia) {
    fun isPropertyAt(index: Int): Boolean {
        val character = state.charAt(index)
        return character == ANCHOR_INDICATOR || character == TAG_INDICATOR
    }

    fun isAliasAt(index: Int): Boolean = state.charAt(index) == ALIAS_INDICATOR

    fun read(): Boolean {
        if (!isPropertyAt(state.position)) return false
        state.builder.startNode(YamlSyntaxKind.PROPERTIES)
        val first = state.peek()
        property()
        val gap = trivia.whitespaceEnd(state.position)
        val hasSecond = gap > state.position && isPropertyAt(gap)
        if (hasSecond) {
            trivia.whitespace()
            reportRepeated(first, state.peek())
            property()
        }
        state.builder.finishNode()
        return true
    }

    fun alias() {
        val end = anchorNameEnd(state.position + 1)
        reportInvalidName(end, "An alias needs a name")
        state.emitTo(YamlSyntaxKind.ALIAS, end)
    }

    private fun property() {
        if (state.peek() == ANCHOR_INDICATOR) anchor()
        if (state.peek() == TAG_INDICATOR) tag()
    }

    private fun anchor() {
        val end = anchorNameEnd(state.position + 1)
        reportInvalidName(end, "An anchor needs a name")
        state.emitTo(YamlSyntaxKind.ANCHOR, end)
    }

    private fun reportInvalidName(end: Int, missing: String) {
        val name = state.text.substring(startIndex = state.position + 1, endIndex = end)
        if (YamlAnchor.isValidName(name)) return
        val message = missing.takeIf { name.isEmpty() } ?: INVALID_NAME
        state.report(YamlDiagnosticCode.InvalidAnchor, state.span(start = state.position, end = end), message)
    }

    private fun tag() {
        val start = state.position
        val end = tagEnd(start)
        if (end < 0) {
            val lineEnd = state.cursor.lineEndOf(start)
            val range = state.span(start = start, end = lineEnd)
            state.report(YamlDiagnosticCode.InvalidTag, range, "A verbatim tag needs a closing >")
            state.emitTo(YamlSyntaxKind.TAG, lineEnd)
            return
        }
        reportInvalidEscapes(start, end)
        if (state.charAt(start + 1) != VERBATIM_START) reportInvalidEncoding(start, end)
        state.emitTo(YamlSyntaxKind.TAG, end)
    }

    private fun tagEnd(start: Int): Int {
        if (state.charAt(start + 1) == VERBATIM_START) return verbatimEnd(start + 2)
        return shorthandEnd(start + 1)
    }

    private fun verbatimEnd(from: Int): Int {
        var end = from
        while (state.charAt(end).isUriCharacter()) {
            end++
        }
        return (end + 1).takeIf { state.charAt(end) == VERBATIM_END } ?: -1
    }

    private fun shorthandEnd(from: Int): Int {
        var handleEnd = from
        while (state.charAt(handleEnd).isWordCharacter()) {
            handleEnd++
        }
        val hasHandle = state.charAt(handleEnd) == TAG_INDICATOR
        val suffixStart = (handleEnd + 1).takeIf { hasHandle } ?: from
        return suffixEnd(suffixStart)
    }

    private fun suffixEnd(from: Int): Int {
        var end = from
        while (state.charAt(end).isTagCharacter()) {
            end++
        }
        return end
    }

    private fun reportInvalidEscapes(start: Int, end: Int) {
        for (index in start until end) {
            val isEscape = state.charAt(index) == PERCENT
            val hasDigits = state.charAt(index + 1).isHexDigit() && state.charAt(index + 2).isHexDigit()
            if (isEscape && !hasDigits) {
                val range = state.span(start = index, end = index + 1)
                state.report(YamlDiagnosticCode.InvalidTag, range, "A % in a tag needs two hex digits")
            }
        }
    }

    private fun reportInvalidEncoding(start: Int, end: Int) {
        val decoded = TagEscapes.decode(state.text.substring(startIndex = start, endIndex = end))
        if (decoded.firstInvalidByte == null) return
        val range = state.span(start = start, end = end)
        state.report(YamlDiagnosticCode.InvalidTag, range, "The percent escapes of a tag are no valid UTF-8")
    }

    private fun reportRepeated(first: Char, second: Char) {
        if (first != second) return
        val range = state.span(start = state.position, end = state.position + 1)
        state.report(YamlDiagnosticCode.DuplicateProperty, range, "A node has at most one anchor and one tag")
    }

    private fun anchorNameEnd(from: Int): Int {
        var end = from
        while (state.charAt(end).isAnchorCharacter()) {
            end++
        }
        return end
    }

    private companion object {
        const val ANCHOR_INDICATOR: Char = '&'
        const val ALIAS_INDICATOR: Char = '*'
        const val TAG_INDICATOR: Char = '!'
        const val VERBATIM_START: Char = '<'
        const val VERBATIM_END: Char = '>'
        const val PERCENT: Char = '%'
        const val INVALID_NAME: String =
            "An anchor name holds a character an anchor cannot hold or is longer than ${YamlAnchor.MAX_NAME_LENGTH}"
    }
}
