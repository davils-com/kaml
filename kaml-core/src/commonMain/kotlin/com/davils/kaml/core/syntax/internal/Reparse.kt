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

package com.davils.kaml.core.syntax.internal

import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.text.edited

internal class Reparse(private val tree: YamlSyntaxTree) {
    private val limits = tree.yaml.configuration.limits

    fun apply(edits: List<TextEdit>): YamlSyntaxTree {
        val text = tree.text.edited(edits)
        val isWithinLength = text.length <= limits.maximumInputLength
        val region = edits.takeIf { isWithinLength }?.let(::regionOf) ?: return YamlSyntaxTree.parse(text, tree.yaml)
        return spliced(region, text, edits.sumOf(TextEdit::delta))
    }

    private fun regionOf(edits: List<TextEdit>): Region? {
        val starts = edits.map { it.range.start }
        val start = starts.minOrNull() ?: return null
        val end = edits.map { it.range.end }.max()
        val children = positioned()
        val first = children.indexOfFirst { child -> isDocumentInside(child, start, end = child.end - 1) }
        val touched = children.indexOfFirst { child -> isDocumentInside(child, end, end = child.end) }
        val last = extended(children, touched)
        val isOrdered = first >= 0 && touched >= first
        val isAligned = isOrdered && isSafeStart(children, first)
        val boundary = boundaryOf(children, first, last)
        val region = Region(children = children, first = first, last = last, boundary = boundary)
        return region.takeIf { isAligned }
    }

    private fun extended(children: List<Positioned>, last: Int): Int {
        val after = children.subList(fromIndex = last + 1, toIndex = children.size)
        val next = after.indexOfFirst { it.element.kind == YamlSyntaxKind.DOCUMENT }
        return children.lastIndex.takeIf { next < 0 } ?: last + next
    }

    private fun isSafeStart(children: List<Positioned>, first: Int): Boolean =
        isLineStart(children[first].start) && isDirectiveSafe(children, first)

    private fun boundaryOf(children: List<Positioned>, first: Int, last: Int): Boundary {
        val isOrdered = first >= 0 && last >= first
        val isPreviousEnded = isOrdered && isPreviousEnded(children, first)
        val isNextStarted = isOrdered && isNextStarted(children, last)
        return Boundary(isPreviousEnded = isPreviousEnded, isNextStarted = isNextStarted)
    }

    private fun isLineStart(offset: Int): Boolean {
        val previous = tree.text.getOrNull(offset - 1) ?: return true
        return previous == '\n' || previous == '\r'
    }

    private fun isDocumentInside(child: Positioned, offset: Int, end: Int): Boolean {
        val isDocument = child.element.kind == YamlSyntaxKind.DOCUMENT
        val isInside = offset > child.start && offset <= end
        return isDocument && isInside
    }

    private fun isDirectiveSafe(children: List<Positioned>, first: Int): Boolean {
        val firstDocument = children[first].element
        val hasDirectives = firstDocument.children.any { it.kind == YamlSyntaxKind.DIRECTIVE }
        return isPreviousEnded(children, first) || !hasDirectives
    }

    private fun isPreviousEnded(children: List<Positioned>, first: Int): Boolean {
        val before = children.subList(fromIndex = 0, toIndex = first)
        val previous = before.lastOrNull { it.element.kind == YamlSyntaxKind.DOCUMENT } ?: return true
        return previous.element.children.any { it.kind == YamlSyntaxKind.DOCUMENT_END }
    }

    private fun isNextStarted(children: List<Positioned>, last: Int): Boolean {
        val next = children.getOrNull(last + 1) ?: return true
        return startsExplicitly(next.element)
    }

    private fun spliced(region: Region, text: String, delta: Int): YamlSyntaxTree {
        val start = region.children[region.first].start
        val oldEnd = region.children[region.last].end
        val parsed = YamlSyntaxSupport.parse(text.substring(startIndex = start, endIndex = oldEnd + delta), tree.yaml)
        val diagnostics = splicedDiagnostics(parsed, Span(start = start, end = oldEnd, delta = delta))
        val isFitting = isBounded(parsed) && isSeparated(parsed, region)
        val isReusable = isFitting && !isAmbiguous(start, oldEnd) && isWithinLimits(diagnostics)
        if (!isReusable) return YamlSyntaxTree.parse(text, tree.yaml)
        val leading = region.children.subList(fromIndex = 0, toIndex = region.first)
        val trailing = region.children.subList(fromIndex = region.last + 1, toIndex = region.children.size)
        val elements = leading.map { it.element } + parsed.root.children + trailing.map { it.element }
        val root = GreenNode(YamlSyntaxKind.STREAM, elements)
        return YamlSyntaxTree(text = text, parsed = YamlSyntaxParse(root, diagnostics), yaml = tree.yaml)
    }

    private fun splicedDiagnostics(parsed: YamlSyntaxParse, span: Span): List<YamlDiagnostic> {
        val before = tree.diagnostics.filter { it.range.start < span.start }
        val after = tree.diagnostics.filter { it.range.start >= span.end }
        val added = parsed.diagnostics.map { shifted(it, span.start) }
        val diagnostics = before + after.map { shifted(it, span.delta) } + added
        return diagnostics.sortedBy { it.range }
    }

    private fun isWithinLimits(diagnostics: List<YamlDiagnostic>): Boolean {
        val isCounted = diagnostics.size <= limits.maximumDiagnostics
        val isUnlimited = tree.diagnostics.none(::isLimit) && diagnostics.none(::isLimit)
        return isCounted && isUnlimited
    }

    private fun isLimit(diagnostic: YamlDiagnostic): Boolean {
        val code = diagnostic.code
        return code.value.startsWith(LIMIT_CODES)
    }

    private fun isAmbiguous(start: Int, end: Int): Boolean = tree.diagnostics.any { diagnostic ->
        val isEmpty = diagnostic.range.length == 0
        val isOnBoundary = diagnostic.range.start == start || diagnostic.range.start == end
        isEmpty && isOnBoundary
    }

    private fun isSeparated(parsed: YamlSyntaxParse, region: Region): Boolean {
        val documents = parsed.root.children.filter { it.kind == YamlSyntaxKind.DOCUMENT }
        val first = documents.firstOrNull() ?: return false
        val isStartSeparated = region.boundary.isPreviousEnded || startsExplicitly(first)
        val isEndSeparated = region.boundary.isNextStarted || endsExplicitly(documents.last())
        return isStartSeparated && isEndSeparated
    }

    private fun startsExplicitly(document: GreenElement): Boolean =
        document.children.take(1).any { it.kind == YamlSyntaxKind.DOCUMENT_START }

    private fun endsExplicitly(document: GreenElement): Boolean =
        document.children.any { it.kind == YamlSyntaxKind.DOCUMENT_END }

    private fun isBounded(parsed: YamlSyntaxParse): Boolean {
        val isOpen = parsed.diagnostics.any { it.code in unbounded }
        val last = lastToken(parsed.root)
        return !isOpen && last.kind != YamlSyntaxKind.BLOCK_SCALAR_CONTENT
    }

    private fun lastToken(root: GreenElement): GreenElement =
        generateSequence(root) { element -> element.children.lastOrNull { it.length > 0 } }.last()

    private fun shifted(diagnostic: YamlDiagnostic, delta: Int): YamlDiagnostic {
        val range = TextRange(start = diagnostic.range.start + delta, end = diagnostic.range.end + delta)
        return diagnostic.movedTo(range)
    }

    private fun positioned(): List<Positioned> {
        var offset = 0
        return tree.green.children.map { child ->
            val start = offset
            offset += child.length
            Positioned(child, start, offset)
        }
    }

    private companion object {
        const val LIMIT_CODES: String = "kaml.limit."

        private val unbounded: Set<YamlDiagnosticCode> = setOf(
            YamlDiagnosticCode.UnterminatedScalar,
            YamlDiagnosticCode.UnterminatedFlowCollection,
            YamlDiagnosticCode.DocumentMarkerInScalar,
            YamlDiagnosticCode.DirectiveWithoutDocument,
        )
    }

    private data class Positioned(val element: GreenElement, val start: Int, val end: Int)

    private data class Region(val children: List<Positioned>, val first: Int, val last: Int, val boundary: Boundary)

    private data class Boundary(val isPreviousEnded: Boolean, val isNextStarted: Boolean)

    private data class Span(val start: Int, val end: Int, val delta: Int)
}
