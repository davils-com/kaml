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
import com.davils.kore.core.text.TextRange

internal class FlowPairs(
    private val state: ParseState,
    private val trivia: Trivia,
    private val nodes: FlowNodes,
) {
    private val builder = state.builder
    private val cursor = state.cursor

    suspend fun Descent.explicitEntry(shape: FlowShape, indent: Int): Boolean {
        val isExplicit = state.peek() == '?' && isSeparatedAt(state.position + 1)
        if (!isExplicit) return false
        builder.startNode(shape.entryKind)
        state.emit(YamlSyntaxKind.MAPPING_KEY, 1)
        separation(indent)
        builder.startNode(YamlSyntaxKind.KEY)
        keyOrEmpty(indent)
        builder.finishNode()
        val indicator = valueIndicatorAfter(state.position, ValueSearch(isMapping = true, isJsonLike = true))
        if (indicator >= 0) valueAt(indicator, indent)
        builder.finishNode()
        return true
    }

    suspend fun Descent.emptyKeyEntry(shape: FlowShape, indent: Int): Boolean {
        val isEmptyKey = state.peek() == ':' && isSeparatedAt(state.position + 1)
        if (!isEmptyKey) return false
        builder.startNode(shape.entryKind)
        builder.startNode(YamlSyntaxKind.KEY)
        nodes.emptyNode()
        builder.finishNode()
        value(indent)
        builder.finishNode()
        return true
    }

    suspend fun Descent.implicitEntry(checkpoint: Int, shape: FlowShape, indent: Int) {
        val keyEnd = state.position
        val keyStart = keyEnd - builder.lengthSince(checkpoint)
        val search = ValueSearch(isMapping = shape.isMapping, isJsonLike = isJsonLike(keyStart))
        val indicator = valueIndicatorAfter(keyEnd, search)
        if (indicator < 0 && !shape.isMapping) return
        builder.startNodeAt(checkpoint, shape.entryKind)
        builder.startNodeAt(checkpoint, YamlSyntaxKind.KEY)
        builder.finishNode()
        if (indicator >= 0 && !shape.isMapping) checkImplicitKey(state.span(start = keyStart, end = keyEnd))
        if (indicator >= 0) valueAt(indicator, indent)
        builder.finishNode()
    }

    fun separation(indent: Int) {
        var hasMoved = true
        while (hasMoved) {
            val before = state.position
            trivia.whitespace()
            if (trivia.isCommentAt(state.position)) trivia.comment()
            if (state.peek().isBreak()) nextFlowLine(indent)
            hasMoved = state.position > before
        }
    }

    private fun checkImplicitKey(range: TextRange) {
        val isMultiLine = cursor.lineEndOf(range.start) < range.end
        if (isMultiLine) {
            state.report(YamlDiagnosticCode.MultilineImplicitKey, range, "An implicit key must be on a single line")
        }
        if (range.length > MAXIMUM_KEY_LENGTH) {
            state.report(
                YamlDiagnosticCode.ImplicitKeyTooLong,
                range,
                "An implicit key is longer than 1024 characters",
            )
        }
    }

    private suspend fun Descent.keyOrEmpty(indent: Int) {
        val character = state.peek()
        val isValueIndicator = character == ':' && isSeparatedAt(state.position + 1)
        val isEnd = isEntryEnd(character) || isValueIndicator
        if (isEnd) {
            nodes.emptyNode()
            return
        }
        with(nodes) { flowNode(indent, FlowContext.FLOW_IN) }
    }

    private suspend fun Descent.valueAt(indicator: Int, indent: Int) {
        trivia.consumeTo(indicator)
        value(indent)
    }

    private suspend fun Descent.value(indent: Int) {
        state.emit(YamlSyntaxKind.MAPPING_VALUE, 1)
        builder.startNode(YamlSyntaxKind.VALUE)
        val target = valueStart(trivia.whitespaceEnd(state.position))
        if (target >= 0) trivia.consumeTo(target)
        val isEmpty = target < 0 || isEntryEnd(state.peek())
        if (isEmpty) nodes.emptyNode()
        if (!isEmpty) with(nodes) { flowNode(indent, FlowContext.FLOW_IN) }
        builder.finishNode()
    }

    private fun valueStart(afterSpace: Int): Int {
        val character = state.charAt(afterSpace)
        val isNode = !isEntryEnd(character) && !character.isBreak() && !trivia.isCommentAt(afterSpace)
        if (isNode) return afterSpace
        return valueOnLaterLine(afterSpace)
    }

    private fun valueOnLaterLine(afterSpace: Int): Int {
        val line = laterLine(afterSpace)
        val isValue = line != null && !line.isDocumentMarker && !isEntryEnd(state.charAt(line.contentStart))
        val start = line?.contentStart ?: -1
        return start.takeIf { isValue } ?: -1
    }

    private fun laterLine(afterSpace: Int): ContentLine? {
        if (!trivia.isLineEndAt(afterSpace)) return null
        return trivia.contentLineFrom(cursor.nextLineStart(afterSpace))
    }

    private fun valueIndicatorAfter(from: Int, search: ValueSearch): Int {
        val afterSpace = trivia.whitespaceEnd(from)
        val sameLine = afterSpace.takeIf { state.charAt(it) == ':' }
        val candidate = sameLine ?: indicatorOnLaterLine(afterSpace, search.isMapping)
        val isAccepted = candidate >= 0 && (search.isJsonLike || isSeparatedAt(candidate + 1))
        return candidate.takeIf { isAccepted } ?: -1
    }

    private fun indicatorOnLaterLine(afterSpace: Int, isMapping: Boolean): Int {
        val line = laterLine(afterSpace).takeIf { isMapping }
        val start = line?.contentStart ?: -1
        return start.takeIf { start >= 0 && state.charAt(start) == ':' } ?: -1
    }

    private fun isJsonLike(keyStart: Int): Boolean {
        var index = keyStart
        while (state.charAt(index) == '&' || state.charAt(index) == '!') {
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

    private fun isEntryEnd(character: Char): Boolean =
        character == ',' || character == ']' || character == '}' || character == END

    private fun isSeparatedAt(index: Int): Boolean {
        val character = state.charAt(index)
        return character.isBlankOrEnd() || character.isFlowIndicator()
    }

    private fun nextFlowLine(indent: Int) {
        trivia.lineBreak()
        if (cursor.isDocumentMarkerAt(state.position)) return
        var spaces = state.position
        while (state.charAt(spaces) == SPACE) {
            spaces++
        }
        val content = trivia.whitespaceEnd(spaces)
        val isBlank = state.charAt(content).isBreak() || state.charAt(content) == END
        if (!isBlank && spaces - state.position < indent) {
            val range = state.span(start = state.position, end = content)
            state.report(YamlDiagnosticCode.InvalidFlowIndentation, range, "This flow line is not indented enough")
        }
    }

    private data class ValueSearch(val isMapping: Boolean, val isJsonLike: Boolean)

    private companion object {
        const val MAXIMUM_KEY_LENGTH: Int = 1024
        const val JSON_STARTS: String = "\"'[{"
    }
}
