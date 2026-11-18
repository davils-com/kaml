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

internal class FlowNodes(
    private val state: ParseState,
    private val trivia: Trivia,
    private val properties: Properties,
    private val plain: PlainScalars,
    private val quoted: QuotedScalars,
) {
    private val builder = state.builder
    private val collections = FlowCollections(state, trivia, this)

    fun isContentAt(index: Int, context: FlowContext): Boolean {
        val character = state.charAt(index)
        val isCollection = character == '[' || character == '{'
        return isCollection ||
            quoted.isQuoteAt(index) ||
            properties.isAliasAt(index) ||
            plain.isPlainStartAt(index, context)
    }

    suspend fun Descent.flowNode(indent: Int, context: FlowContext) {
        val checkpoint = builder.checkpoint()
        val hasProperties = properties.read()
        if (hasProperties) separateBeforeContent(indent, context)
        contentInto(checkpoint = checkpoint, indent = indent, context = context, hasProperties = hasProperties)
    }

    suspend fun Descent.contentInto(checkpoint: Int, indent: Int, context: FlowContext, hasProperties: Boolean) {
        val kind = contentKind(state.position, context)
        builder.startNodeAt(checkpoint, kind)
        val isCollection = kind == YamlSyntaxKind.FLOW_SEQUENCE || kind == YamlSyntaxKind.FLOW_MAPPING
        if (isCollection) with(collections) { collection(indent) }
        if (kind == YamlSyntaxKind.ALIAS_NODE) alias(hasProperties)
        if (kind == YamlSyntaxKind.SCALAR) scalar(indent, context)
        builder.finishNode()
    }

    fun emptyNode() {
        builder.startNode(YamlSyntaxKind.EMPTY_NODE)
        builder.finishNode()
    }

    private fun separateBeforeContent(indent: Int, context: FlowContext) {
        val afterSpace = trivia.whitespaceEnd(state.position)
        val isAdjacent = afterSpace == state.position && !trivia.isLineEndAt(afterSpace)
        if (isAdjacent) return
        val target = contentAfterSeparation(afterSpace, indent, context)
        if (target >= 0) trivia.consumeTo(target)
    }

    private fun contentAfterSeparation(afterSpace: Int, indent: Int, context: FlowContext): Int {
        if (isContentAt(afterSpace, context)) return afterSpace
        val line = laterLine(afterSpace, context)
        val isAccepted = line != null &&
            !line.isDocumentMarker &&
            line.indent >= indent &&
            isContentAt(line.contentStart, context)
        val start = line?.contentStart ?: -1
        return start.takeIf { isAccepted } ?: -1
    }

    private fun laterLine(afterSpace: Int, context: FlowContext): ContentLine? {
        val isOpen = context.isMultiLine && trivia.isLineEndAt(afterSpace)
        if (!isOpen) return null
        return trivia.contentLineFrom(state.cursor.nextLineStart(afterSpace))
    }

    private fun contentKind(index: Int, context: FlowContext): YamlSyntaxKind =
        collectionKinds[state.charAt(index)] ?: scalarKind(index, context)

    private fun scalarKind(index: Int, context: FlowContext): YamlSyntaxKind {
        if (properties.isAliasAt(index)) return YamlSyntaxKind.ALIAS_NODE
        val isScalar = quoted.isQuoteAt(index) || plain.isPlainStartAt(index, context)
        return YamlSyntaxKind.SCALAR.takeIf { isScalar } ?: YamlSyntaxKind.EMPTY_NODE
    }

    private fun alias(hasProperties: Boolean) {
        if (hasProperties) {
            val range = state.span(start = state.position, end = state.position + 1)
            state.report(YamlDiagnosticCode.AliasWithProperties, range, "An alias cannot have properties")
        }
        properties.alias()
    }

    private fun scalar(indent: Int, context: FlowContext) {
        if (quoted.isQuoteAt(state.position)) {
            quoted.quoted(indent, context)
            return
        }
        plain.plain(indent, context)
    }

    private companion object {
        val collectionKinds: Map<Char, YamlSyntaxKind> = mapOf(
            '[' to YamlSyntaxKind.FLOW_SEQUENCE,
            '{' to YamlSyntaxKind.FLOW_MAPPING,
        )
    }
}
