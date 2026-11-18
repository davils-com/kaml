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

package com.davils.kaml.core.emit

import com.davils.kaml.core.model.YamlCollectionStyle

internal class BlockWriter(
    private val options: YamlEmitOptions,
    private val out: OutputBuffer,
    tags: TagNotation,
    resolution: PlainResolution,
) {
    private val styler = ScalarStyler(options, resolution)
    private val flow = FlowWriter(styler, tags)
    private val lineBreak = options.lineBreak.text

    fun root(node: EmitNode, isAfterMarker: Boolean) {
        if (isInline(node, ScalarContext.DOCUMENT)) {
            if (isAfterMarker) out.append(' ')
            flow.inline(node, ScalarContext.DOCUMENT, out)
            out.append(lineBreak)
            return
        }
        val properties = flow.properties(node)
        val hasHeader = isAfterMarker || properties.isNotEmpty()
        if (isAfterMarker) appendProperties(properties)
        if (!isAfterMarker) out.append(properties)
        nested(node, Placement(indent = 0, parentIndent = ROOT), startsLine = !hasHeader)
    }

    fun comments(lines: List<String>, indent: Int) {
        lines.forEach { line ->
            out.spaces(indent)
            out.append(COMMENT_PREFIX)
            if (line.isNotEmpty()) out.append(' ')
            out.append(line)
            out.append(lineBreak)
        }
    }

    private fun nested(node: EmitNode, placement: Placement, startsLine: Boolean) {
        val scalar = node as? EmitNode.Scalar
        if (scalar != null) {
            blockScalar(scalar, placement, startsLine)
            return
        }
        if (!startsLine) out.append(lineBreak)
        if (node is EmitNode.Mapping) mapping(node, placement.indent)
        if (node is EmitNode.Sequence) sequence(node, placement.indent)
    }

    private fun mapping(node: EmitNode.Mapping, indent: Int) {
        node.entries.forEachIndexed { index, (key, value) ->
            comments(node.comments.getOrNull(index).orEmpty(), indent)
            out.spaces(indent)
            entry(key, value, indent)
        }
    }

    private fun compactMapping(node: EmitNode.Mapping, column: Int) {
        node.entries.forEachIndexed { index, (key, value) ->
            if (index > 0) {
                comments(node.comments.getOrNull(index).orEmpty(), column)
                out.spaces(column)
            }
            entry(key, value, column)
        }
    }

    private fun entry(key: EmitNode, value: EmitNode, indent: Int) {
        val simpleKey = simpleKeyOf(key)
        if (simpleKey == null) {
            out.append('?')
            compact(key, indent)
            out.spaces(indent)
            out.append(':')
            compact(value, indent)
            return
        }
        out.append(simpleKey)
        val isPropertyLast = simpleKey.isNotEmpty() && flow.isEmptyBody(key, ScalarContext.BLOCK_KEY)
        if (key is EmitNode.Alias || isPropertyLast) out.append(' ')
        out.append(':')
        value(value, indent)
    }

    private fun simpleKeyOf(key: EmitNode): String? {
        if (!isInline(key, ScalarContext.BLOCK_KEY)) return null
        val probe = OutputBuffer(MAXIMUM_KEY_LENGTH, isStrict = false)
        flow.inline(key, ScalarContext.BLOCK_KEY, probe)
        return probe.toString().takeIf { !probe.isOverflowed }
    }

    private fun value(node: EmitNode, indent: Int) {
        if (isInline(node, ScalarContext.BLOCK)) {
            inlineValue(node)
            return
        }
        appendProperties(flow.properties(node))
        val nested = indent + options.indent
        val sequenceIndent = nested.takeIf { options.isSequenceIndented } ?: indent
        val childIndent = sequenceIndent.takeIf { node is EmitNode.Sequence } ?: nested
        nested(node, Placement(indent = childIndent, parentIndent = indent), startsLine = false)
    }

    private fun sequence(node: EmitNode.Sequence, indent: Int) {
        node.items.forEach { item ->
            out.spaces(indent)
            out.append('-')
            compact(item, indent)
        }
    }

    private fun compactSequence(node: EmitNode.Sequence, column: Int) {
        node.items.forEachIndexed { index, item ->
            if (index > 0) out.spaces(column)
            out.append('-')
            compact(item, column)
        }
    }

    private fun compact(node: EmitNode, column: Int) {
        if (isInline(node, ScalarContext.BLOCK)) {
            inlineValue(node)
            return
        }
        val properties = flow.properties(node)
        if (properties.isEmpty() && isCompactable(node)) {
            out.append(' ')
            compactCollection(node, column + COMPACT_OFFSET)
            return
        }
        appendProperties(properties)
        nested(node, Placement(indent = column + options.indent, parentIndent = column), startsLine = false)
    }

    private fun isCompactable(node: EmitNode): Boolean {
        if (node is EmitNode.Mapping) return node.comments.firstOrNull().isNullOrEmpty()
        return node is EmitNode.Sequence
    }

    private fun compactCollection(node: EmitNode, column: Int) {
        if (node is EmitNode.Mapping) compactMapping(node, column)
        if (node is EmitNode.Sequence) compactSequence(node, column)
    }

    private fun inlineValue(node: EmitNode) {
        val isEmpty = flow.properties(node).isEmpty() && flow.isEmptyBody(node, ScalarContext.BLOCK)
        if (!isEmpty) out.append(' ')
        flow.inline(node, ScalarContext.BLOCK, out)
        out.append(lineBreak)
    }

    private fun blockScalar(node: EmitNode.Scalar, placement: Placement, startsLine: Boolean) {
        val parentIndent = maxOf(a = placement.parentIndent, b = 0)
        val contentIndent = maxOf(a = placement.indent, b = parentIndent + options.indent)
        if (!startsLine) out.append(' ')
        out.append(styler.blockHeader(node, width = contentIndent - parentIndent))
        out.append(lineBreak)
        styler.blockLines(node, contentIndent, out)
    }

    private fun appendProperties(properties: String) {
        if (properties.isEmpty()) return
        out.append(' ')
        out.append(properties)
    }

    private fun isInline(node: EmitNode, context: ScalarContext): Boolean = when (node) {
        is EmitNode.Alias -> true
        is EmitNode.Scalar -> !styler.isBlock(node, context)
        is EmitNode.Sequence -> node.items.isEmpty() || isFlowStyled(node.style)
        is EmitNode.Mapping -> node.entries.isEmpty() || isFlowStyled(node.style)
    }

    private fun isFlowStyled(style: YamlCollectionStyle): Boolean =
        style == YamlCollectionStyle.Flow && options.isStylePreserved

    private data class Placement(val indent: Int, val parentIndent: Int)

    private companion object {
        const val ROOT: Int = -1
        const val COMMENT_PREFIX: Char = '#'
        const val COMPACT_OFFSET: Int = 2
        const val MAXIMUM_KEY_LENGTH: Int = 1024
    }
}
