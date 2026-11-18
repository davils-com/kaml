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

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.syntax.YamlSyntaxNode
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.ast.YamlEntrySyntax
import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange

internal class EditWriter(tree: YamlSyntaxTree, document: Int) {
    val lineBreak: String = "\r\n".takeIf { it in tree.text } ?: "\n"
    val text: ValueText = ValueText(tree.yaml, lineBreak, tree.documents.getOrNull(document)?.version)
    val layout: NodeLayout = NodeLayout(tree.text, tree.lines)

    fun replaced(old: YamlValueSyntax?, value: YamlNode, slot: Slot): EditStep {
        if (old == null) return unsupported()
        val node = old.node
        return when (slot.isFlow) {
            true -> written(text.flow(value)) { flow -> TextEdit(node.range, flow) }
            false -> blockReplacement(node, value, slot)
        }
    }

    fun inserted(mapping: YamlValueSyntax.Mapping, name: String, value: YamlNode): EditStep {
        val key = text.key(name) ?: return unsupported()
        val entries = mapping.entries
        return when (mapping.isFlow) {
            true -> written(text.flow(value)) { flow ->
                layout.flowInsertion(
                    collection = mapping.node,
                    last = entries.lastOrNull()?.node,
                    item = "$key: $flow",
                )
            }

            false -> EditStep.Edits(blockInsertion(entries.last(), key, value))
        }
    }

    fun appended(sequence: YamlValueSyntax.Sequence, value: YamlNode): EditStep {
        val items = sequence.items
        if (!sequence.isFlow) return EditStep.Edits(blockAppend(layout.hostOf(items.last().node), value))
        return written(text.flow(value)) { flow ->
            layout.flowInsertion(collection = sequence.node, last = items.lastOrNull()?.node, item = flow)
        }
    }

    fun insertedBefore(sequence: YamlValueSyntax.Sequence, item: YamlValueSyntax, value: YamlNode): EditStep {
        val start = TextRange.at(item.node.range.start)
        if (sequence.isFlow) return written(text.flow(value)) { flow -> TextEdit(start, "$flow, ") }
        return blockInsertionBefore(layout.hostOf(item.node), value)
    }

    private fun written(flow: String?, edit: (String) -> TextEdit): EditStep {
        val written = flow ?: return unsupported()
        return EditStep.Edits(edit(written))
    }

    private fun blockInsertionBefore(host: YamlSyntaxNode, value: YamlNode): EditStep {
        val lineStart = layout.lineStartOf(host) ?: return unsupported()
        val column = layout.columnOf(host)
        val rendering = text.item(value, column)
        val inserted = " ".repeat(column) + "-" + rendering.head + rendering.tail + lineBreak
        return EditStep.Edits(TextEdit(TextRange.at(lineStart), inserted))
    }

    private fun blockReplacement(node: YamlSyntaxNode, value: YamlNode, slot: Slot): EditStep {
        val gap = layout.gapBefore(node)
        val end = maxOf(a = gap.start, b = layout.contentEnd(node))
        val rendering = renderingOf(value, slot, gap.hasComment)
        val trailing = layout.restOfLine(end)
        val isMoved = trailing.isNotBlank() && rendering.tail.isNotEmpty()
        val isComment = trailing.trimStart().startsWith("#")
        if (isMoved && !isComment) return unsupported()
        val moved = trailing.takeIf { isMoved }.orEmpty()
        val range = TextRange(start = gap.start, end = end + moved.length)
        return EditStep.Edits(TextEdit(range, rendering.head + moved + rendering.tail))
    }

    private fun renderingOf(value: YamlNode, slot: Slot, hasComment: Boolean): ValueText.Rendering {
        if (hasComment) return text.onNewLine(value, slot.column)
        return when (slot.isItem) {
            true -> text.item(value, slot.column)
            false -> text.afterIndicator(value, slot.column)
        }
    }

    private fun blockInsertion(last: YamlEntrySyntax, key: String, value: YamlNode): TextEdit {
        val rendering = text.afterIndicator(value, last.column)
        val inserted = lineBreak + " ".repeat(last.column) + key + ":" + rendering.head + rendering.tail
        return TextEdit(TextRange.at(layout.lineEndAfter(last.node)), inserted)
    }

    private fun blockAppend(entry: YamlSyntaxNode, value: YamlNode): TextEdit {
        val column = layout.columnOf(entry)
        val rendering = text.item(value, column)
        val inserted = lineBreak + " ".repeat(column) + "-" + rendering.head + rendering.tail
        return TextEdit(TextRange.at(layout.lineEndAfter(entry)), inserted)
    }

    private fun unsupported(): EditStep = EditStep.Refused(YamlEditRefusal.Unsupported)

    internal data class Slot(val column: Int, val isFlow: Boolean, val isItem: Boolean)
}
