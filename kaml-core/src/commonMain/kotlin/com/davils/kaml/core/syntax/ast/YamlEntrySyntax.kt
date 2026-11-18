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

package com.davils.kaml.core.syntax.ast

import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.YamlSyntaxNode

/**
 * The typed view of an entry of a mapping: its key, its value and the comments around it.
 *
 * The view is immutable and safe to share between threads. Keys, values and comments come from the input unchanged
 * and must be escaped before they are shown.
 *
 * @property node the syntax node of the entry: a [YamlSyntaxKind.BLOCK_MAPPING_ENTRY], a
 * [YamlSyntaxKind.FLOW_MAPPING_ENTRY] or a [YamlSyntaxKind.FLOW_PAIR].
 * @since 1.0.0
 */
public class YamlEntrySyntax internal constructor(
    public val node: YamlSyntaxNode,
    private val index: CommentIndex,
) {
    /**
     * The zero-based column the entry starts at, in UTF-16 code units.
     *
     * @since 1.0.0
     */
    public val column: Int get() = index.lines.positionOf(node.range.start).column

    /**
     * The key of the entry; [YamlValueSyntax.Empty] for an entry without a key, such as `: value`.
     *
     * @since 1.0.0
     */
    public val key: YamlValueSyntax by lazy {
        val wrappers = node.childNodes.filter { it.kind == YamlSyntaxKind.KEY }
        SyntaxValues.wrapped((wrappers + node).first(), column, index)
    }

    /**
     * The value of the entry, or `null` for an explicit key without a value, such as `? key`.
     *
     * @since 1.0.0
     */
    public val value: YamlValueSyntax? by lazy {
        val wrapper = node.child(YamlSyntaxKind.VALUE)
        wrapper?.let { SyntaxValues.wrapped(it, column, index) }
    }

    /**
     * The text of the key when it is a scalar, with escapes and line folding resolved; `null` for any other key.
     *
     * @since 1.0.0
     */
    public val keyText: String?
        get() {
            val scalar = key as? YamlValueSyntax.Scalar ?: return null
            return scalar.value
        }

    /**
     * The comment lines directly above the entry, without their `#` and the space after it, from top to bottom.
     *
     * A blank line or a line with content ends the comments that belong to the entry.
     *
     * @since 1.0.0
     */
    public val leadingComments: List<String> get() = index.above(index.lines.lineOf(node.range.start))

    /**
     * The comment at the end of the first line of the entry, without its `#` and the space after it, or `null` if
     * that line has none.
     *
     * @since 1.0.0
     */
    public val trailingComment: String?
        get() {
            val line = index.lines.lineOf(node.range.start)
            return index.trailingOn(line)
        }
}
