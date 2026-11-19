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

package com.davils.kaml.document.redact.rule

import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.YamlSyntaxNode
import com.davils.kaml.core.syntax.YamlSyntaxToken
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.internal.OrderedGroups
import com.davils.kaml.document.internal.OrderedIndex
import com.davils.kaml.document.patch.syntax.ContentRanges
import com.davils.kore.core.text.LineMap
import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange

private const val COMMENT_MARK: String = "#"
private const val MERGE_KEY: String = "<<"
private const val EMPTY_MAPPING: String = "{}"
private val entryKinds: Set<YamlSyntaxKind> = setOf(
    YamlSyntaxKind.BLOCK_MAPPING_ENTRY,
    YamlSyntaxKind.FLOW_MAPPING_ENTRY,
    YamlSyntaxKind.FLOW_PAIR,
)
private val itemKinds: Set<YamlSyntaxKind> = entryKinds + YamlSyntaxKind.BLOCK_SEQUENCE_ENTRY

internal class RedactionEdits(
    tree: YamlSyntaxTree,
    private val rules: RedactionRules,
    private val targets: List<YamlSyntaxNode>,
) {
    private val covered = Coverage(targets.map(ContentRanges::of))
    private val lines = LineMap.of(tree.text)
    private val targetLines = Coverage(targets.map(::linesOf))
    private val tokens: List<YamlSyntaxToken> = tree.root.tokens.toList()
    private val anchors = AnchorDefinitions(tree)
    private val quoted: String = DoubleQuoted.of(rules.replacement)

    fun all(walk: SyntaxWalk): List<TextEdit> {
        val replaced = covered.ranges.map { range -> TextEdit(range, quoted) }
        val scalars = walk.scalars.mapNotNull(::scalarEdit)
        val rewritten = Coverage(scalars.map(TextEdit::range))
        val edits = replaced + tagEdits() + scalars + commentEdits(rewritten) + aliasEdits() + anchoredKeyEdits(walk)
        return edits.sortedBy { edit -> edit.range.start }
    }

    private fun tagEdits(): List<TextEdit> {
        val tags = targets.mapNotNull { node -> node.child(YamlSyntaxKind.PROPERTIES)?.token(YamlSyntaxKind.TAG) }
        val distinct = OrderedGroups<TextRange, YamlSyntaxToken>()
        tags.filter { tag -> !covered.contains(tag.range) }.forEach { tag -> distinct.add(tag.range, tag) }
        return distinct.keys.map { range -> TextEdit(range, "") }
    }

    private fun scalarEdit(scalar: YamlValueSyntax.Scalar): TextEdit? {
        if (!rules.hasValuePatterns) return null
        val range = ContentRanges.of(scalar.node)
        val value = scalar.value.takeIf { !covered.contains(range) }
        val redacted = value?.let { text -> rules.value(text) ?: throw YamlDocumentLimitException(PATTERN_INPUT) }
        return redacted?.takeIf { it != value }?.let { text -> TextEdit(range, DoubleQuoted.of(text)) }
    }

    private fun commentEdits(rewritten: Coverage): List<TextEdit> {
        val edits = ArrayList<TextEdit>()
        var lastEnd = -1
        tokens.forEach { token ->
            val isComment = token.kind == YamlSyntaxKind.COMMENT && !rewritten.contains(token.range)
            if (isComment) commentEdit(token, lastEnd)?.let(edits::add)
            if (token.kind == YamlSyntaxKind.LINE_BREAK) lastEnd = -1
            if (!token.kind.isTrivia) lastEnd = token.range.end
        }
        return edits
    }

    private fun commentEdit(comment: YamlSyntaxToken, lastEnd: Int): TextEdit? {
        if (covered.contains(comment.range)) return null
        val isOnTargetLine = covered.endsAt(lastEnd) || targetLines.contains(lineOf(comment.range.start))
        val isTrailing = rules.isTrailingCommentRedacted && isOnTargetLine
        val body = comment.text.removePrefix(COMMENT_MARK)
        val replaced = " " + rules.replacement
        val redacted = replaced.takeIf { isTrailing } ?: patterned(body)
        return redacted.takeIf { it != body }?.let { text -> TextEdit(comment.range, COMMENT_MARK + text) }
    }

    private fun linesOf(node: YamlSyntaxNode): TextRange {
        val item = node.ancestors.firstOrNull { ancestor -> ancestor.kind in itemKinds } ?: node
        val first = lines.lineOf(item.range.start)
        val last = lines.lineOf(ContentRanges.of(node).end)
        return TextRange(start = first, end = last + 1)
    }

    private fun lineOf(offset: Int): TextRange {
        val line = lines.lineOf(offset)
        return TextRange(start = line, end = line + 1)
    }

    private fun patterned(body: String): String {
        if (!rules.hasCommentPatterns) return body
        return rules.comment(body) ?: throw YamlDocumentLimitException(PATTERN_INPUT)
    }

    private fun aliasEdits(): List<TextEdit> {
        val removed = OrderedIndex<String>()
        val coveredAnchors = tokens.filter { it.kind == YamlSyntaxKind.ANCHOR && covered.contains(it.range) }
        coveredAnchors.forEach { anchor -> removed.ordinalFor(anchor.text.drop(1)) }
        val aliases = tokens.filter { it.kind == YamlSyntaxKind.ALIAS && !covered.contains(it.range) }
        val orphans = aliases.filter { alias -> removed.ordinalOf(alias.text.drop(1)) != null }
        return orphans.map { alias -> TextEdit(alias.range, EMPTY_MAPPING.takeIf { isMerged(alias) } ?: quoted) }
    }

    private fun anchoredKeyEdits(walk: SyntaxWalk): List<TextEdit> {
        if (!rules.hasValuePatterns) return emptyList()
        val uncovered = walk.keyScalars.filter { key -> !covered.contains(ContentRanges.of(key.node)) }
        val keys = OrderedGroups<TextRange, YamlValueSyntax.Scalar>()
        uncovered.forEach { key -> keys.add(key.node.range, key) }
        val aliases = tokens.filter { it.kind == YamlSyntaxKind.ALIAS && !covered.contains(it.range) && !isKey(it) }
        return aliases.mapNotNull { alias ->
            val key = anchors.rangeOf(alias.text.drop(1), alias.range.start)?.let(keys::get)
            key?.let { anchored -> anchoredKeyEdit(alias, anchored.first()) }
        }
    }

    private fun anchoredKeyEdit(alias: YamlSyntaxToken, key: YamlValueSyntax.Scalar): TextEdit? {
        val redacted = rules.value(key.value) ?: throw YamlDocumentLimitException(PATTERN_INPUT)
        return TextEdit(alias.range, DoubleQuoted.of(redacted)).takeIf { redacted != key.value }
    }

    private fun isKey(alias: YamlSyntaxToken): Boolean = alias.ancestors.take(2).any { it.kind == YamlSyntaxKind.KEY }

    private fun isMerged(alias: YamlSyntaxToken): Boolean {
        val chain = alias.ancestors.toList()
        val entryAt = chain.indexOfFirst { node -> node.kind in entryKinds }
        val isValue = chain.getOrNull(entryAt - 1)?.kind == YamlSyntaxKind.VALUE
        val key = chain.getOrNull(entryAt)?.child(YamlSyntaxKind.KEY)
        return isValue && key != null && key.text.trim() == MERGE_KEY
    }

    private companion object {
        const val PATTERN_INPUT: String = "maximumPatternInputLength"
    }
}
