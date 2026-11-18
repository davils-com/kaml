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

internal class BlockCollections(
    private val state: ParseState,
    private val trivia: Trivia,
    private val blocks: BlockNodes,
    private val keys: ImplicitKeys,
) {
    private val builder = state.builder
    private val cursor = state.cursor

    fun shapeAt(index: Int): Shape? {
        if (isSequenceEntryAt(index)) return Shape.SEQUENCE
        return Shape.MAPPING.takeIf { isMappingEntryAhead() }
    }

    suspend fun Descent.collection(shape: Shape, column: Int, checkpoint: Int) {
        builder.startNodeAt(checkpoint, shape.kind)
        var hasMore = true
        while (hasMore) {
            if (shape == Shape.SEQUENCE) sequenceEntry(column)
            if (shape == Shape.MAPPING) mappingEntry(column)
            hasMore = nextEntry(column, shape)
        }
        builder.finishNode()
    }

    fun isSequenceEntryAt(index: Int): Boolean = isIndicatorAt(index, '-')

    private suspend fun Descent.sequenceEntry(column: Int) {
        builder.startNode(YamlSyntaxKind.BLOCK_SEQUENCE_ENTRY)
        state.emit(YamlSyntaxKind.SEQUENCE_ENTRY, 1)
        with(blocks) { blockNode(Frame(parentIndent = column, isBlockOut = false), Placement.COMPACT) }
        builder.finishNode()
    }

    private fun nextEntry(column: Int, shape: Shape): Boolean {
        trivia.finishLine()
        var line = trivia.nextContentLine()
        while (line != null && !line.isDocumentMarker && line.indent > column) {
            skipOverIndented(line)
            line = trivia.nextContentLine()
        }
        val isSequenceLine = line != null && isSequenceEntryAt(line.contentStart)
        val isEntry = shape == Shape.MAPPING || isSequenceLine
        val isContinued = line != null && !line.isDocumentMarker && line.indent == column && isEntry
        if (line == null || !isContinued) return false
        if (line.hasTabIndentation) trivia.reportTab(line)
        trivia.consumeTo(line.contentStart)
        return true
    }

    private fun skipOverIndented(line: ContentLine) {
        trivia.consumeTo(line.contentStart)
        val end = cursor.lineEndOf(state.position)
        val range = state.span(start = state.position, end = end)
        state.report(YamlDiagnosticCode.BadIndentation, range, "This line is indented more than its block")
        state.unexpected(end, "Unexpected content")
    }

    private suspend fun Descent.mappingEntry(column: Int) {
        builder.startNode(YamlSyntaxKind.BLOCK_MAPPING_ENTRY)
        val isHandled = explicitEntry(column) || emptyKeyEntry(column) || implicitEntry(column)
        if (!isHandled) state.unexpected(cursor.lineEndOf(state.position), "Expected a mapping entry")
        builder.finishNode()
    }

    private suspend fun Descent.explicitEntry(column: Int): Boolean {
        if (!isIndicatorAt(state.position, '?')) return false
        state.emit(YamlSyntaxKind.MAPPING_KEY, 1)
        builder.startNode(YamlSyntaxKind.KEY)
        with(blocks) { blockNode(Frame(parentIndent = column, isBlockOut = true), Placement.COMPACT) }
        builder.finishNode()
        explicitValue(column)
        return true
    }

    private suspend fun Descent.explicitValue(column: Int) {
        trivia.finishLine()
        val line = trivia.nextContentLine()
        val hasValue = line != null &&
            !line.isDocumentMarker &&
            line.indent == column &&
            isIndicatorAt(line.contentStart, ':')
        if (line == null || !hasValue) return
        trivia.consumeTo(line.contentStart)
        value(column, Placement.COMPACT)
    }

    private suspend fun Descent.emptyKeyEntry(column: Int): Boolean {
        if (!isIndicatorAt(state.position, ':')) return false
        builder.startNode(YamlSyntaxKind.KEY)
        blocks.emptyNode()
        builder.finishNode()
        value(column, Placement.VALUE)
        return true
    }

    private suspend fun Descent.implicitEntry(column: Int): Boolean {
        if (!keys.isImplicitKeyAhead()) return false
        builder.startNode(YamlSyntaxKind.KEY)
        with(keys) { implicitKey(column) }
        builder.finishNode()
        trivia.whitespace()
        value(column, Placement.VALUE)
        return true
    }

    private suspend fun Descent.value(column: Int, placement: Placement) {
        state.emit(YamlSyntaxKind.MAPPING_VALUE, 1)
        builder.startNode(YamlSyntaxKind.VALUE)
        with(blocks) { blockNode(Frame(parentIndent = column, isBlockOut = true), placement) }
        builder.finishNode()
    }

    private fun isMappingEntryAhead(): Boolean =
        isIndicatorAt(state.position, '?') || isIndicatorAt(state.position, ':') || keys.isImplicitKeyAhead()

    private fun isIndicatorAt(index: Int, indicator: Char): Boolean =
        state.charAt(index) == indicator && state.charAt(index + 1).isBlankOrEnd()

    enum class Shape(val kind: YamlSyntaxKind) {
        SEQUENCE(YamlSyntaxKind.BLOCK_SEQUENCE),
        MAPPING(YamlSyntaxKind.BLOCK_MAPPING),
    }
}
