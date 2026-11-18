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
import com.davils.kaml.core.parser.BlockNodes.Frame
import com.davils.kaml.core.parser.BlockNodes.Placement
import com.davils.kaml.core.syntax.YamlSyntaxKind

internal class Documents(private val state: ParseState, private val trivia: Trivia, private val blocks: BlockNodes) {
    private val builder = state.builder
    private val cursor = state.cursor
    private val root = Frame(parentIndent = -1, isBlockOut = false)

    fun stream() {
        builder.startNode(YamlSyntaxKind.STREAM)
        var isDirectiveAllowed = true
        var line = streamPrefix()
        while (line != null) {
            isDirectiveAllowed = streamStep(line, isDirectiveAllowed)
            line = streamPrefix()
        }
        trivia.consumeTo(state.text.length, isStreamPrefix = true)
        builder.finishNode()
    }

    private fun streamStep(line: ContentLine, isDirectiveAllowed: Boolean): Boolean {
        trivia.consumeTo(line.contentStart, isStreamPrefix = true)
        if (!isEndMarkerAt(state.position)) return document(isDirectiveAllowed)
        state.emit(YamlSyntaxKind.DOCUMENT_END, Cursor.MARKER_LENGTH)
        trivia.finishLine()
        return true
    }

    private fun streamPrefix(): ContentLine? {
        while (isPrefixAhead()) {
            prefixStep()
        }
        return trivia.nextContentLine()
    }

    private fun isPrefixAhead(): Boolean {
        val isBlankLine = cursor.isFreshLine && trivia.isLineEndAt(state.position) && state.peek() != END
        return state.peek() == BYTE_ORDER_MARK || isBlankLine
    }

    private fun prefixStep() {
        if (state.peek() == BYTE_ORDER_MARK) {
            state.emit(YamlSyntaxKind.BYTE_ORDER_MARK, 1)
            return
        }
        trivia.whitespace()
        if (trivia.isCommentAt(state.position)) trivia.comment()
        if (state.peek().isBreak()) trivia.lineBreak()
    }

    private fun document(isDirectiveAllowed: Boolean): Boolean {
        builder.startNode(YamlSyntaxKind.DOCUMENT)
        val hasDirectives = isDirectiveAllowed && directives()
        val isExplicit = isStartMarkerAt(state.position)
        if (hasDirectives && !isExplicit) {
            val range = state.span(start = state.position, end = state.position)
            state.report(YamlDiagnosticCode.DirectiveWithoutDocument, range, "Directives must be followed by ---")
        }
        if (isExplicit) state.emit(YamlSyntaxKind.DOCUMENT_START, Cursor.MARKER_LENGTH)
        val placement = Placement.DOCUMENT.takeIf { isExplicit } ?: Placement.LINE_START
        state.descend { with(blocks) { blockNode(root, placement) } }
        val isEnded = documentSuffix()
        builder.finishNode()
        return isEnded
    }

    private fun directives(): Boolean {
        var hasFound = false
        while (state.peek() == DIRECTIVE_INDICATOR && cursor.column == 0) {
            directive()
            hasFound = true
            val line = trivia.nextContentLine() ?: break
            trivia.consumeTo(line.contentStart)
        }
        return hasFound
    }

    private fun directive() {
        builder.startNode(YamlSyntaxKind.DIRECTIVE)
        state.emitTo(YamlSyntaxKind.DIRECTIVE_NAME, wordEnd(state.position))
        trivia.whitespace()
        while (!trivia.isLineEndAt(state.position)) {
            state.emitTo(YamlSyntaxKind.DIRECTIVE_PARAMETER, wordEnd(state.position))
            trivia.whitespace()
        }
        trivia.finishLine()
        builder.finishNode()
    }

    private fun documentSuffix(): Boolean {
        trivia.finishLine()
        var line = trivia.nextContentLine()
        while (line != null && !line.isDocumentMarker) {
            trivia.consumeTo(line.contentStart)
            state.unexpected(cursor.lineEndOf(state.position), "Unexpected content after the document")
            line = trivia.nextContentLine()
        }
        if (line == null || !isEndMarkerAt(line.contentStart)) return false
        trivia.consumeTo(line.contentStart)
        state.emit(YamlSyntaxKind.DOCUMENT_END, Cursor.MARKER_LENGTH)
        trivia.finishLine()
        return true
    }

    private fun wordEnd(from: Int): Int {
        var end = from
        while (!state.charAt(end).isBlankOrEnd()) {
            end++
        }
        return end
    }

    private fun isStartMarkerAt(index: Int): Boolean = cursor.isDocumentMarkerAt(index) && state.charAt(index) == '-'

    private fun isEndMarkerAt(index: Int): Boolean = cursor.isDocumentMarkerAt(index) && state.charAt(index) == '.'

    private companion object {
        const val DIRECTIVE_INDICATOR: Char = '%'
    }
}
