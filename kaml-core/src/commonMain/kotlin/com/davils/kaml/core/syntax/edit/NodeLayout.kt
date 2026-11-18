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

package com.davils.kaml.core.syntax.edit

import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.YamlSyntaxNode
import com.davils.kore.core.text.LineMap
import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange

internal class NodeLayout(private val text: String, private val lines: LineMap) {
    fun columnOf(node: YamlSyntaxNode): Int = lines.positionOf(node.range.start).column

    fun hostOf(node: YamlSyntaxNode): YamlSyntaxNode = node.ancestors.first()

    fun gapBefore(node: YamlSyntaxNode): Gap {
        val host = hostOf(node)
        val gap = host.children.takeWhile { it != node }
        val comment = gap.lastOrNull { it.kind == YamlSyntaxKind.COMMENT }
        val indicator = gap.lastOrNull { it.kind == YamlSyntaxKind.SEQUENCE_ENTRY }
        val start = (comment ?: indicator)?.run { range.end } ?: host.range.start
        return Gap(start = start, hasComment = comment != null)
    }

    fun contentEnd(node: YamlSyntaxNode): Int {
        val last = node.tokens.lastOrNull { !it.kind.isTrivia } ?: return node.range.start
        val breaks = last.text.length - last.text.trimEnd('\n', '\r').length
        return last.range.end - breaks
    }

    fun restOfLine(offset: Int): String {
        val end = lines.lineRange(lines.lineOf(offset)).end
        return text.substring(startIndex = offset, endIndex = maxOf(a = offset, b = end))
    }

    fun lineEndAfter(node: YamlSyntaxNode): Int {
        val last = maxOf(a = node.range.start, b = contentEnd(node) - 1)
        return lines.lineRange(lines.lineOf(last)).end
    }

    fun lineStartOf(node: YamlSyntaxNode): Int? {
        val start = lines.lineRange(lines.lineOf(node.range.start)).start
        return start.takeIf { text.substring(startIndex = start, endIndex = node.range.start).isBlank() }
    }

    fun afterLineBreak(offset: Int): Int {
        val lineBreak = lineBreaks.firstOrNull { text.startsWith(it, offset) }.orEmpty()
        return offset + lineBreak.length
    }

    fun flowInsertion(collection: YamlSyntaxNode, last: YamlSyntaxNode?, item: String): TextEdit {
        val previous = last ?: return TextEdit(TextRange.at(openingEnd(collection)), item)
        return TextEdit(TextRange.at(contentEnd(previous)), ", $item")
    }

    fun flowRemoval(siblings: List<YamlSyntaxNode>, target: YamlSyntaxNode): TextEdit {
        val index = siblings.indexOf(target)
        val next = siblings.getOrNull(index + 1)
        val previous = siblings.getOrNull(index - 1)?.takeIf { next == null }
        val start = previous?.let(::contentEnd) ?: target.range.start
        val end = next?.run { range.start } ?: contentEnd(target)
        return TextEdit(TextRange(start, end), "")
    }

    private fun openingEnd(collection: YamlSyntaxNode): Int {
        val opening = collection.tokens.first { it.kind in openings }
        return opening.range.end
    }

    internal data class Gap(val start: Int, val hasComment: Boolean)

    private companion object {
        private val lineBreaks: List<String> = listOf("\r\n", "\n", "\r")

        private val openings: Set<YamlSyntaxKind> = setOf(
            YamlSyntaxKind.FLOW_SEQUENCE_START,
            YamlSyntaxKind.FLOW_MAPPING_START,
        )
    }
}
