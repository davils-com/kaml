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

internal class BlockNodes(
    private val state: ParseState,
    private val trivia: Trivia,
    private val properties: Properties,
    private val flow: FlowNodes,
    private val blockScalars: BlockScalars,
) {
    private val builder = state.builder
    private val cursor = state.cursor
    private val collections = BlockCollections(
        state = state,
        trivia = trivia,
        blocks = this,
        keys = ImplicitKeys(state = state, trivia = trivia, properties = properties, flow = flow),
    )

    suspend fun Descent.blockNode(frame: Frame, placement: Placement) {
        with(state) { nested { nestedBlockNode(frame, placement) } }
    }

    fun emptyNode() {
        flow.emptyNode()
    }

    private suspend fun Descent.nestedBlockNode(frame: Frame, placement: Placement) {
        if (placement != Placement.LINE_START) trivia.whitespace()
        val checkpoint = builder.checkpoint()
        if (trivia.isLineEndAt(state.position)) {
            laterLines(frame, checkpoint, hasProperties = false)
            return
        }
        sameLine(frame, placement, checkpoint)
    }

    private suspend fun Descent.sameLine(frame: Frame, placement: Placement, checkpoint: Int) {
        val isTabbed = isTabbedBefore(state.position)
        val isCompact = placement == Placement.COMPACT && !isTabbed
        val isCollectionAllowed = placement == Placement.LINE_START || isCompact
        if (isCollectionAllowed && collection(checkpoint, isTabbed)) return
        val hasProperties = properties.read()
        if (hasProperties && trivia.isLineEndAt(state.position)) {
            laterLines(frame, checkpoint, hasProperties = true)
            return
        }
        if (hasProperties) trivia.whitespace()
        inlineContent(frame, checkpoint, hasProperties)
    }

    private suspend fun Descent.collection(checkpoint: Int, isTabbed: Boolean): Boolean {
        val shape = collections.shapeAt(state.position) ?: return false
        if (isTabbed) reportTab()
        with(collections) { collection(shape, cursor.column, checkpoint) }
        return true
    }

    private fun reportTab() {
        val range = state.span(start = cursor.lineStart, end = state.position)
        state.report(YamlDiagnosticCode.TabIndentation, range, "Tabs cannot indent a block collection")
    }

    private fun isTabbedBefore(index: Int): Boolean {
        var probe = index - 1
        var isTabbed = false
        while (probe >= cursor.lineStart && state.charAt(probe).isWhite()) {
            isTabbed = isTabbed || state.charAt(probe) == TAB
            probe--
        }
        return isTabbed
    }

    private suspend fun Descent.inlineContent(frame: Frame, checkpoint: Int, hasProperties: Boolean) {
        if (blockScalars.isBlockScalarAt(state.position)) {
            builder.startNodeAt(checkpoint, YamlSyntaxKind.BLOCK_SCALAR)
            blockScalars.blockScalar(frame.parentIndent)
            builder.finishNode()
            return
        }
        with(flow) {
            contentInto(
                checkpoint = checkpoint,
                indent = frame.parentIndent + 1,
                context = FlowContext.FLOW_OUT,
                hasProperties = hasProperties,
            )
        }
        trivia.finishLine()
    }

    private suspend fun Descent.laterLines(frame: Frame, checkpoint: Int, hasProperties: Boolean) {
        if (!isLaterLineAccepted(frame, checkpoint)) return
        if (!hasProperties) {
            sameLine(frame, Placement.LINE_START, builder.checkpoint())
            return
        }
        var isContinued = true
        while (isContinued) {
            isContinued = afterProperties(frame, checkpoint)
        }
    }

    private suspend fun Descent.afterProperties(frame: Frame, checkpoint: Int): Boolean {
        if (collection(checkpoint, isTabbedBefore(state.position))) return false
        val hasMore = properties.read()
        val isLineEnd = hasMore && trivia.isLineEndAt(state.position)
        if (!isLineEnd) {
            if (hasMore) trivia.whitespace()
            inlineContent(frame, checkpoint, hasProperties = true)
        }
        return isLineEnd && isLaterLineAccepted(frame, checkpoint)
    }

    private fun isLaterLineAccepted(frame: Frame, checkpoint: Int): Boolean {
        trivia.finishLine()
        val line = trivia.nextContentLine()
        val isAccepted = line != null && !line.isDocumentMarker && line.indent >= minimumIndent(line, frame)
        if (line == null || !isAccepted) {
            emptyAt(checkpoint)
            return false
        }
        trivia.consumeTo(line.contentStart)
        return true
    }

    private fun minimumIndent(line: ContentLine, frame: Frame): Int {
        val isCompactSequence = frame.isBlockOut && collections.isSequenceEntryAt(line.contentStart)
        val allowance = 1.takeIf { isCompactSequence } ?: 0
        return frame.parentIndent + 1 - allowance
    }

    private fun emptyAt(checkpoint: Int) {
        builder.startNodeAt(checkpoint, YamlSyntaxKind.EMPTY_NODE)
        builder.finishNode()
    }

    data class Frame(val parentIndent: Int, val isBlockOut: Boolean)

    enum class Placement {
        COMPACT,
        VALUE,
        DOCUMENT,
        LINE_START,
    }
}
