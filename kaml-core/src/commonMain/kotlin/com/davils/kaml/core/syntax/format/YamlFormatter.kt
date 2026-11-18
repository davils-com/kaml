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

import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.validation.violation.Severity

/**
 * Formats the layout of YAML documents without changing what they mean.
 *
 * The formatter indents block collections by [YamlFormatOptions.indent], removes trailing whitespace, collapses runs
 * of blank lines and adds a final line break; comments move with the line below them. It never touches the content
 * of scalars, flow collections or the order of entries. Before it returns, it composes the original and the
 * formatted text and refuses with [YamlFormatRefusal.MeaningChanged] unless both give the same nodes and the same
 * diagnostics, so formatting fails closed.
 *
 * A formatter is immutable and safe to share between threads. Its work grows linearly with the length of the text,
 * which the limits of the configuration of the tree bound; composing applies every limit and budget of that
 * configuration.
 *
 * @property options how the formatter lays out documents.
 * @since 1.0.0
 */
public class YamlFormatter(public val options: YamlFormatOptions = YamlFormatOptions.Default) {
    /**
     * Formats [tree], or only the lines [range] touches.
     *
     * @param tree the tree to format, which is not changed.
     * @param range the range of the text whose lines to format, or `null` to format the whole text.
     * @return the formatted tree with the edits that produced it, or the reason the formatter refused:
     * [YamlFormatRefusal.SyntaxErrors] if [tree] has errors, [YamlFormatRefusal.MeaningChanged] if the formatted text
     * would mean something else.
     * @throws IllegalArgumentException if [range] reaches beyond the text of [tree].
     * @since 1.0.0
     */
    public fun format(tree: YamlSyntaxTree, range: TextRange? = null): YamlFormatResult {
        val length = tree.text.length
        val end = range?.end ?: 0
        require(end <= length) { "The range ends at $end, after the end of the text at $length" }
        val hasErrors = tree.diagnostics.any { it.severity == Severity.Error }
        if (hasErrors) return YamlFormatResult.Refused(YamlFormatRefusal.SyntaxErrors)
        val lines = range?.let { linesOf(tree, it) }
        val layout = LineLayout(tree, options)
        val edits = layout.edits().filter { edit -> lines == null || touches(edit = edit.range, range = lines) }
        val formatted = tree.edit(edits)
        val isSame = hasSameMeaning(before = tree, after = formatted)
        val result = YamlFormatResult.Formatted(edits, formatted)
        return result.takeIf { isSame } ?: YamlFormatResult.Refused(YamlFormatRefusal.MeaningChanged)
    }

    override fun toString(): String = "YamlFormatter($options)"

    private fun linesOf(tree: YamlSyntaxTree, range: TextRange): TextRange {
        val map = tree.lines
        val first = map.lineRange(map.lineOf(range.start))
        val last = map.lineRange(map.lineOf(range.end))
        return TextRange(start = first.start, end = last.end)
    }

    private fun touches(edit: TextRange, range: TextRange): Boolean = edit.start <= range.end && range.start <= edit.end

    private fun hasSameMeaning(before: YamlSyntaxTree, after: YamlSyntaxTree): Boolean {
        val original = before.compose()
        val changed = after.compose()
        val isSameOutcome = original::class == changed::class && codesOf(original) == codesOf(changed)
        return isSameOutcome && rootsOf(original) == rootsOf(changed)
    }

    private fun codesOf(result: YamlParseResult): List<YamlDiagnosticCode> = result.diagnostics.map { it.code }

    private fun rootsOf(result: YamlParseResult): List<YamlNode> {
        val stream = when (result) {
            is YamlParseResult.Parsed -> result.stream
            is YamlParseResult.Failed -> result.partial
        }
        return stream.documents.map { it.root }
    }
}
