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
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.ast.YamlEntrySyntax
import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kaml.core.syntax.edit.YamlPath.Segment
import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange

internal class EntryAnnotating(private val tree: YamlSyntaxTree, private val writer: EditWriter) {
    private val layout = writer.layout

    fun rename(parent: YamlValueSyntax, segment: Segment, key: String): EditStep {
        val written = writer.text.key(key)
        val entry = EditPaths.entryOf(parent, segment) ?: return EditStep.Refused(YamlEditRefusal.MissingPath)
        val node = entry.key.node
        val edit = written?.let { TextEdit(TextRange(start = node.range.start, end = layout.contentEnd(node)), it) }
        return edit?.let { EditStep.Edits(it) } ?: EditStep.Refused(YamlEditRefusal.Unsupported)
    }

    fun comment(parent: YamlValueSyntax, segment: Segment, comment: List<String>): EditStep {
        val entry = EditPaths.entryOf(parent, segment) ?: return EditStep.Refused(YamlEditRefusal.MissingPath)
        return commented(entry, comment)
    }

    private fun commented(entry: YamlEntrySyntax, comment: List<String>): EditStep {
        val isFlow = entry.node.kind != YamlSyntaxKind.BLOCK_MAPPING_ENTRY
        if (isFlow) return EditStep.Refused(YamlEditRefusal.Unsupported)
        val lines = tree.lines
        val start = lines.lineRange(lines.lineOf(entry.node.range.start)).start
        val pad = " ".repeat(entry.column)
        val inserted = comment.joinToString("") { line -> pad + commentLine(line) + writer.lineBreak }
        return EditStep.Edits(TextEdit(TextRange.at(start), inserted))
    }

    private fun commentLine(line: String): String = "#" + " $line".takeIf { line.isNotEmpty() }.orEmpty()
}
