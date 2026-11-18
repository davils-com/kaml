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

package com.davils.kaml.core.syntax.format

import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.YamlSyntaxNode
import com.davils.kaml.core.syntax.YamlSyntaxToken
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange

internal class LineLayout(private val tree: YamlSyntaxTree, private val options: YamlFormatOptions) {
    private val text = tree.text
    private val lines = tree.lines
    private val lineBreak = "\r\n".takeIf { it in text } ?: "\n"
    private val maximumLength = tree.yaml.configuration.limits.maximumOutputLength
    private var inserted = 0L
    private val shifts = ArrayList<ShiftIndex.Shift>()
    private val gaps = ArrayList<TextEdit>()
    private val blockContent: RangeIndex
    private val flowScalars: RangeIndex
    private val nextContentLines: IntArray = nextContentLinesOf()
    private val shiftIndex: ShiftIndex = ShiftIndex(shiftsOf())

    init {
        val tokens = tree.root.tokens.toList()
        blockContent = RangeIndex(rangesOf(tokens) { it == YamlSyntaxKind.BLOCK_SCALAR_CONTENT })
        flowScalars = RangeIndex(rangesOf(tokens) { it in flowScalarKinds })
    }

    fun edits(): List<TextEdit>? {
        val edits = ArrayList<TextEdit>(gaps)
        var blanks = 0
        var line = 0
        while (line < lines.lineCount && !isTooLong()) {
            blanks = lineEdits(line, blanks, edits)
            line++
        }
        finalBreak()?.let(edits::add)
        return edits.sortedBy { it.range.start }.takeIf { !isTooLong() }
    }

    private fun isTooLong(): Boolean = inserted > maximumLength

    private fun charged(edit: TextEdit): TextEdit {
        inserted += edit.replacement.length
        return edit
    }

    private fun rangesOf(tokens: List<YamlSyntaxToken>, isWanted: (YamlSyntaxKind) -> Boolean): List<TextRange> {
        val wanted = tokens.filter { token -> isWanted(token.kind) }
        return wanted.map { it.range }
    }

    private fun nextContentLinesOf(): IntArray {
        val next = IntArray(lines.lineCount)
        var following = NONE
        for (line in lines.lineCount - 1 downTo 0) {
            next[line] = following
            following = line.takeIf { isContentLine(it) } ?: following
        }
        return next
    }

    private fun shiftsOf(): List<ShiftIndex.Shift> {
        val roots = tree.documents.map { document -> document.root.node }
        roots.forEach(::visitAll)
        return shifts
    }

    private fun visitAll(root: YamlSyntaxNode) {
        val pending = ArrayList<Visit>()
        pending.add(Visit(root, desired = 0, inherited = 0))
        while (pending.isNotEmpty()) {
            val next = pending.removeAt(pending.lastIndex)
            pending.addAll(visit(next).asReversed())
        }
    }

    private fun visit(visit: Visit): List<Visit> {
        val node = visit.node
        if (node.kind !in blockCollections) return emptyList()
        val actual = lines.positionOf(node.range.start).column
        val lineStart = lines.lineRange(lines.lineOf(node.range.start)).start
        val isLineStart = text.substring(startIndex = lineStart, endIndex = node.range.start).isBlank()
        val indicatorEnd = compactIndicatorEnd(node).takeIf { !isLineStart }
        val placement = Placement(
            wanted = visit.desired - actual,
            inherited = visit.inherited,
            isLineStart = isLineStart,
            indicatorEnd = indicatorEnd,
        )
        val shift = shiftOf(node, placement)
        val column = actual + shift
        return node.childNodes.flatMap { entry -> children(entry, column, shift) }
    }

    private fun shiftOf(node: YamlSyntaxNode, placement: Placement): Int {
        val end = placement.indicatorEnd
        if (placement.isLineStart || end == null) return lineShift(node, placement)
        return compactShift(node, placement, end)
    }

    private fun lineShift(node: YamlSyntaxNode, placement: Placement): Int {
        if (!placement.isLineStart) return placement.inherited
        shifts.add(ShiftIndex.Shift(node.range, placement.wanted))
        return placement.wanted
    }

    private fun compactShift(node: YamlSyntaxNode, placement: Placement, end: Int): Int {
        val gap = node.range.start - end
        val spaces = maxOf(a = 1, b = gap + placement.wanted - placement.inherited)
        val shift = placement.inherited + spaces - gap
        shifts.add(ShiftIndex.Shift(node.range, shift))
        val isWritten = spaces != gap && !isTooLong()
        if (isWritten) gaps.add(charged(TextEdit(TextRange(start = end, end = node.range.start), " ".repeat(spaces))))
        return shift
    }

    private fun compactIndicatorEnd(node: YamlSyntaxNode): Int? {
        val indicator = indicatorOf(node) ?: return null
        val gap = text.substring(startIndex = indicator.range.end, endIndex = node.range.start)
        val isSpaces = gap.all { character -> character == ' ' }
        return indicator.range.end.takeIf { isSpaces }
    }

    private fun indicatorOf(node: YamlSyntaxNode): YamlSyntaxToken? {
        val entry = node.ancestors.first()
        val isSequenceEntry = entry.kind == YamlSyntaxKind.BLOCK_SEQUENCE_ENTRY
        return entry.token(YamlSyntaxKind.SEQUENCE_ENTRY).takeIf { isSequenceEntry }
    }

    private fun children(entry: YamlSyntaxNode, column: Int, shift: Int): List<Visit> {
        val wrappers = entry.childNodes.filter { it.kind in wrapperKinds }
        val values = (wrappers.flatMap { it.childNodes } + entry.childNodes).filter { it.kind.isValueNode }
        val isSequenceEntry = entry.kind == YamlSyntaxKind.BLOCK_SEQUENCE_ENTRY
        return values.map { value ->
            val isFlush =
                value.kind == YamlSyntaxKind.BLOCK_SEQUENCE && !options.isSequenceIndented && !isSequenceEntry
            val extra = 0.takeIf { isFlush } ?: options.indent
            Visit(value, desired = column + extra, inherited = shift)
        }
    }

    private fun lineEdits(line: Int, blanks: Int, edits: MutableList<TextEdit>): Int {
        val range = lines.lineRange(line)
        val content = text.substring(startIndex = range.start, endIndex = range.end)
        val isVerbatim = isVerbatimAt(range.start)
        val isBlank = !isVerbatim && content.isBlank()
        if (isBlank) return blank(line = line, range = range, blanks = blanks, edits = edits)
        indentation(line, content, isVerbatim)?.let { edit -> edits.add(charged(edit)) }
        val isTrimmed = !isVerbatim && !isInFlowScalar(range.end)
        if (isTrimmed) trailing(range, content)?.let(edits::add)
        return 0
    }

    private fun isVerbatimAt(offset: Int): Boolean {
        val isInBlockScalar = blockContent.containing(offset) != null
        return isInBlockScalar || isInFlowScalar(offset)
    }

    private fun isInFlowScalar(offset: Int): Boolean {
        val scalar = flowScalars.containing(offset) ?: return false
        return offset > scalar.start
    }

    private fun blank(line: Int, range: TextRange, blanks: Int, edits: MutableList<TextEdit>): Int {
        val isExtra = blanks >= options.maximumEmptyLines && line + 1 < lines.lineCount
        if (isExtra) edits.add(TextEdit(TextRange(start = range.start, end = lines.lineRange(line + 1).start), ""))
        if (!isExtra && range.length > 0) edits.add(TextEdit(range, ""))
        return blanks + 1
    }

    private fun indentation(line: Int, content: String, isVerbatim: Boolean): TextEdit? {
        val start = lines.lineRange(line).start
        val spaces = spacesOf(line)
        val target = targetOf(line, content, isVerbatim)
        val isChanged = target != spaces && content.isNotBlank()
        if (!isChanged) return null
        return TextEdit(TextRange(start, start + spaces), " ".repeat(target))
    }

    private fun targetOf(line: Int, content: String, isVerbatim: Boolean): Int {
        val isComment = !isVerbatim && content.trimStart().startsWith("#")
        val next = nextContentLines[line].takeIf { it != NONE }
        val anchor = next.takeIf { isComment } ?: line
        val target = spacesOf(anchor) + shiftIndex.shiftAt(lines.lineRange(anchor).start + spacesOf(anchor))
        return maxOf(a = 0, b = target)
    }

    private fun spacesOf(line: Int): Int {
        val range = lines.lineRange(line)
        var end = range.start
        while (end < range.end && text[end] == ' ') {
            end++
        }
        return end - range.start
    }

    private fun trailing(range: TextRange, content: String): TextEdit? {
        val trimmed = content.trimEnd(' ', '\t').length
        if (trimmed == content.length) return null
        return TextEdit(TextRange(start = range.start + trimmed, end = range.end), "")
    }

    private fun isContentLine(line: Int): Boolean {
        val range = lines.lineRange(line)
        val content = text.substring(startIndex = range.start, endIndex = range.end).trimStart()
        return content.isNotEmpty() && !content.startsWith("#")
    }

    private fun finalBreak(): TextEdit? {
        val hasBreak = text.endsWith("\n") || text.endsWith("\r")
        val isMissing = options.hasFinalLineBreak && text.isNotEmpty() && !hasBreak
        if (!isMissing) return null
        return charged(TextEdit(TextRange.at(text.length), lineBreak))
    }

    private data class Visit(val node: YamlSyntaxNode, val desired: Int, val inherited: Int)

    private data class Placement(val wanted: Int, val inherited: Int, val isLineStart: Boolean, val indicatorEnd: Int?)

    private companion object {
        const val NONE: Int = -1

        private val blockCollections: Set<YamlSyntaxKind> = setOf(
            YamlSyntaxKind.BLOCK_MAPPING,
            YamlSyntaxKind.BLOCK_SEQUENCE,
        )

        private val wrapperKinds: Set<YamlSyntaxKind> = setOf(YamlSyntaxKind.KEY, YamlSyntaxKind.VALUE)

        private val flowScalarKinds: Set<YamlSyntaxKind> = setOf(
            YamlSyntaxKind.PLAIN_SCALAR,
            YamlSyntaxKind.SINGLE_QUOTED_SCALAR,
            YamlSyntaxKind.DOUBLE_QUOTED_SCALAR,
        )
    }
}
