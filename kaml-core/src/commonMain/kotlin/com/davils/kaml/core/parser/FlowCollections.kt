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

internal class FlowCollections(
    private val state: ParseState,
    trivia: Trivia,
    private val nodes: FlowNodes,
) {
    private val builder = state.builder
    private val cursor = state.cursor
    private val pairs = FlowPairs(state, trivia, nodes)

    suspend fun Descent.collection(indent: Int) {
        with(state) { nested { nestedCollection(indent) } }
    }

    private suspend fun Descent.nestedCollection(indent: Int) {
        val shape = FlowShape.of(state.peek())
        val start = state.position
        state.emit(shape.startKind, 1)
        var isOpen = true
        while (isOpen) {
            isOpen = entryStep(indent, shape, start)
        }
    }

    private suspend fun Descent.entryStep(indent: Int, shape: FlowShape, start: Int): Boolean {
        pairs.separation(indent)
        val isFinished = !state.hasNodeBudget() || closed(shape) || unterminated(start)
        if (isFinished) return false
        val before = state.position
        if (!emptyEntry()) entryWithSeparator(indent, shape, before)
        return true
    }

    private fun closed(shape: FlowShape): Boolean {
        if (state.peek() != shape.closer) return false
        state.emit(shape.endKind, 1)
        return true
    }

    private fun unterminated(start: Int): Boolean {
        val isEnd = state.peek() == END || cursor.isDocumentMarkerAt(state.position)
        if (!isEnd) return false
        val range = state.span(start = start, end = state.position)
        state.report(YamlDiagnosticCode.UnterminatedFlowCollection, range, "A flow collection is not closed")
        return true
    }

    private fun emptyEntry(): Boolean {
        if (state.peek() != ',') return false
        val range = state.span(start = state.position, end = state.position + 1)
        state.report(YamlDiagnosticCode.UnexpectedContent, range, "A flow collection entry is missing")
        state.emit(YamlSyntaxKind.FLOW_ENTRY_SEPARATOR, 1)
        return true
    }

    private suspend fun Descent.entryWithSeparator(indent: Int, shape: FlowShape, before: Int) {
        entry(indent, shape)
        pairs.separation(indent)
        if (separator()) return
        val next = state.peek()
        val isEnd = next == shape.closer || next == END || cursor.isDocumentMarkerAt(state.position)
        if (isEnd) return
        val range = state.span(start = state.position, end = state.position + 1)
        state.report(YamlDiagnosticCode.MissingFlowSeparator, range, "Expected , or ${shape.closer}")
        recover(before, shape)
    }

    private fun separator(): Boolean {
        if (state.peek() != ',') return false
        state.emit(YamlSyntaxKind.FLOW_ENTRY_SEPARATOR, 1)
        return true
    }

    private fun recover(before: Int, shape: FlowShape) {
        val character = state.peek()
        val isCloser = character == ']' || character == '}'
        val isForeignCloser = isCloser && character != shape.closer
        if (isForeignCloser || state.position == before) {
            state.unexpected(state.position + 1, "Unexpected character in a flow collection")
        }
    }

    private suspend fun Descent.entry(indent: Int, shape: FlowShape) {
        val checkpoint = builder.checkpoint()
        val isHandled = with(pairs) { explicitEntry(shape, indent) || emptyKeyEntry(shape, indent) }
        if (isHandled) return
        with(nodes) { flowNode(indent, FlowContext.FLOW_IN) }
        with(pairs) { implicitEntry(checkpoint, shape, indent) }
    }
}
