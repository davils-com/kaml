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

package com.davils.kaml.core.syntax

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.syntax.ast.CommentIndex
import com.davils.kaml.core.syntax.ast.YamlDocumentSyntax
import com.davils.kaml.core.syntax.internal.GreenNode
import com.davils.kaml.core.syntax.internal.Reparse
import com.davils.kaml.core.syntax.internal.YamlSyntaxParse
import com.davils.kaml.core.syntax.internal.YamlSyntaxSupport
import com.davils.kore.core.text.LineMap
import com.davils.kore.core.text.TextEdit

/**
 * The lossless syntax tree of a YAML stream: every character of [text], comments, whitespace and line breaks
 * included, belongs to exactly one token, so the text of [root] is [text] again.
 *
 * A tree is read with [parse], changed with [edit], viewed as documents, entries and values through [documents] and
 * composed into nodes with [compose]. It never throws on malformed YAML: every problem is reported in [diagnostics],
 * and the text the parser could not read becomes a node of kind [YamlSyntaxKind.ERROR].
 *
 * The limits of [yaml] apply before any work is done. A text longer than `YamlLimits.maximumInputLength` is not
 * parsed: its tree holds no tokens, so its [root] does not cover [text], and [diagnostics] holds a single
 * `InputTooLong` diagnostic. Nesting deeper than `YamlLimits.maximumDepth` stops the parser with a `TooDeep`
 * diagnostic and more nodes than `YamlLimits.maximumNodes` with a `TooManyNodes` diagnostic, each leaving the rest
 * of the text in an error node, and at most `YamlLimits.maximumDiagnostics` diagnostics are reported. The same limits apply to an incremental [edit].
 *
 * A tree is immutable and safe to share between threads; [root], [lines] and [documents] are computed once, when
 * they are first asked for. The [text] and the text of every element come from the input unchanged and must be
 * escaped before they are shown.
 *
 * @property text the source text of the tree, exactly as it was given or edited.
 * @property yaml the configuration the text was parsed with, which [edit] and [compose] use as well.
 * @since 1.0.0
 */
public class YamlSyntaxTree internal constructor(
    public val text: String,
    private val parsed: YamlSyntaxParse,
    public val yaml: Yaml,
) {
    internal val green: GreenNode get() = parsed.root

    /**
     * The problems found while the text was parsed, ordered by their range; a read-only copy.
     *
     * @since 1.0.0
     */
    public val diagnostics: List<YamlDiagnostic> = buildList { addAll(parsed.diagnostics) }

    /**
     * The root node of the tree, of kind [YamlSyntaxKind.STREAM].
     *
     * @since 1.0.0
     */
    public val root: YamlSyntaxNode by lazy { YamlSyntaxNode(green, 0, null) }

    /**
     * The lines of [text], which turn offsets into lines and columns.
     *
     * @since 1.0.0
     */
    public val lines: LineMap by lazy { LineMap.of(text) }

    /**
     * The typed view of each document of the stream, in the order of the text.
     *
     * @since 1.0.0
     */
    public val documents: List<YamlDocumentSyntax> by lazy {
        val index = CommentIndex(root, lines, text)
        val nodes = root.childNodes.filter { it.kind == YamlSyntaxKind.DOCUMENT }
        buildList { nodes.forEach { node -> add(YamlDocumentSyntax(node, index)) } }
    }

    /**
     * Applies [edits] to the text and returns the tree of the edited text.
     *
     * Only the documents the edits touch are parsed again; the documents before and after them are shared with this
     * tree. The result has the same text, tokens and diagnostics as a [parse] of the edited text, and when a shared
     * document could be read differently, such as after an edit that opens a quoted scalar or joins the marker of the
     * next document to the line before it, that document or everything is parsed again. The limits of the whole
     * stream, such as `YamlLimits.maximumDocuments` and `YamlLimits.maximumNodes`, are charged over the whole edited
     * tree, not only over the documents parsed again. An edited text longer than `YamlLimits.maximumInputLength` is
     * not parsed at all: the result holds a single `InputTooLong` diagnostic, like [parse].
     *
     * @param edits the edits to apply, in any order, with ranges into [text] that do not overlap.
     * @return the tree of the edited text, parsed with [yaml].
     * @throws IllegalArgumentException if two edits overlap, an edit reaches beyond [text], or the edited text would
     * be too long to be held in a string.
     * @since 1.0.0
     */
    public fun edit(edits: List<TextEdit>): YamlSyntaxTree = Reparse(this).apply(edits)

    /**
     * Composes the documents of this tree into nodes, as [Yaml.parse] does for its text, without parsing again.
     *
     * Every limit and budget of [yaml] applies while the nodes are built, and the diagnostics of the tree are
     * reported with those of the composer.
     *
     * @return the composed stream, or the diagnostics and the partial stream if the text has errors.
     * @since 1.0.0
     */
    public fun compose(): YamlParseResult = YamlSyntaxSupport.compose(
        yaml = yaml,
        text = text,
        root = green,
        diagnostics = diagnostics,
    )

    override fun toString(): String = "YamlSyntaxTree(${text.length} characters, ${diagnostics.size} diagnostics)"

    /**
     * Creates syntax trees.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Parses [text] into its lossless syntax tree.
         *
         * Malformed YAML never throws: its problems are reported in [YamlSyntaxTree.diagnostics]. A text longer than
         * `YamlLimits.maximumInputLength` of [yaml] is refused before it is read, with a tree that holds no tokens
         * and a single `InputTooLong` diagnostic.
         *
         * @param text the YAML text to parse, which may come from an attacker.
         * @param yaml the configuration whose limits apply, [Yaml.Default] by default.
         * @return the syntax tree of [text].
         * @since 1.0.0
         */
        public fun parse(text: String, yaml: Yaml = Yaml.Default): YamlSyntaxTree {
            val parsed = YamlSyntaxSupport.parse(text, yaml)
            return YamlSyntaxTree(text = text, parsed = parsed, yaml = yaml)
        }
    }
}
