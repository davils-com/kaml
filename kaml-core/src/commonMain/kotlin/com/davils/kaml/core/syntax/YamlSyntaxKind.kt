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

/**
 * The kind of a [YamlSyntaxElement]: a token kind for the pieces of text the parser reads, or a node kind for the
 * constructs of the YAML grammar they make up.
 *
 * @property isToken whether elements of this kind are [YamlSyntaxToken]s rather than [YamlSyntaxNode]s.
 * @property isTrivia whether tokens of this kind carry no content: whitespace, line breaks, comments and byte order
 * marks.
 * @since 1.0.0
 */
public enum class YamlSyntaxKind(public val isToken: Boolean, public val isTrivia: Boolean = false) {
    /**
     * A byte order mark at the start of a document.
     *
     * @since 1.0.0
     */
    BYTE_ORDER_MARK(isToken = true, isTrivia = true),

    /**
     * A run of spaces or tabs that separates or indents other tokens.
     *
     * @since 1.0.0
     */
    WHITESPACE(isToken = true, isTrivia = true),

    /**
     * A line break: a line feed, a carriage return or both.
     *
     * @since 1.0.0
     */
    LINE_BREAK(isToken = true, isTrivia = true),

    /**
     * A comment, from its `#` to the end of its line.
     *
     * @since 1.0.0
     */
    COMMENT(isToken = true, isTrivia = true),

    /**
     * The name of a directive with its `%`, such as `%YAML`.
     *
     * @since 1.0.0
     */
    DIRECTIVE_NAME(isToken = true),

    /**
     * A parameter of a directive, such as the version of `%YAML 1.2`.
     *
     * @since 1.0.0
     */
    DIRECTIVE_PARAMETER(isToken = true),

    /**
     * The document start marker `---`.
     *
     * @since 1.0.0
     */
    DOCUMENT_START(isToken = true),

    /**
     * The document end marker `...`.
     *
     * @since 1.0.0
     */
    DOCUMENT_END(isToken = true),

    /**
     * The indicator `-` of an entry of a block sequence.
     *
     * @since 1.0.0
     */
    SEQUENCE_ENTRY(isToken = true),

    /**
     * The indicator `?` of an explicit mapping key.
     *
     * @since 1.0.0
     */
    MAPPING_KEY(isToken = true),

    /**
     * The indicator `:` that separates a key from its value.
     *
     * @since 1.0.0
     */
    MAPPING_VALUE(isToken = true),

    /**
     * The `[` that opens a flow sequence.
     *
     * @since 1.0.0
     */
    FLOW_SEQUENCE_START(isToken = true),

    /**
     * The `]` that closes a flow sequence.
     *
     * @since 1.0.0
     */
    FLOW_SEQUENCE_END(isToken = true),

    /**
     * The `{` that opens a flow mapping.
     *
     * @since 1.0.0
     */
    FLOW_MAPPING_START(isToken = true),

    /**
     * The `}` that closes a flow mapping.
     *
     * @since 1.0.0
     */
    FLOW_MAPPING_END(isToken = true),

    /**
     * The `, ` between the entries of a flow collection.
     *
     * @since 1.0.0
     */
    FLOW_ENTRY_SEPARATOR(isToken = true),

    /**
     * An anchor with its `&`, such as `&base`.
     *
     * @since 1.0.0
     */
    ANCHOR(isToken = true),

    /**
     * An alias with its `*`, such as `*base`.
     *
     * @since 1.0.0
     */
    ALIAS(isToken = true),

    /**
     * A tag as it is written, such as `!!str`, `!local` or `!<tag:example.com,2026:x>`.
     *
     * @since 1.0.0
     */
    TAG(isToken = true),

    /**
     * The text of a plain scalar, which may span several lines.
     *
     * @since 1.0.0
     */
    PLAIN_SCALAR(isToken = true),

    /**
     * The text of a single-quoted scalar, its quotes included.
     *
     * @since 1.0.0
     */
    SINGLE_QUOTED_SCALAR(isToken = true),

    /**
     * The text of a double-quoted scalar, its quotes included.
     *
     * @since 1.0.0
     */
    DOUBLE_QUOTED_SCALAR(isToken = true),

    /**
     * The header of a block scalar: `|` or `>` with its indentation and chomping indicators.
     *
     * @since 1.0.0
     */
    BLOCK_SCALAR_HEADER(isToken = true),

    /**
     * The content lines of a block scalar, their indentation and line breaks included.
     *
     * @since 1.0.0
     */
    BLOCK_SCALAR_CONTENT(isToken = true),

    /**
     * Text the parser could not read; a diagnostic of the tree describes the problem.
     *
     * @since 1.0.0
     */
    UNEXPECTED(isToken = true),

    /**
     * The root of a tree: the whole stream with all its documents.
     *
     * @since 1.0.0
     */
    STREAM(isToken = false),

    /**
     * A document with its directives, markers and root node.
     *
     * @since 1.0.0
     */
    DOCUMENT(isToken = false),

    /**
     * A directive, such as `%YAML 1.2` or `%TAG ! tag:example.com,2026:`.
     *
     * @since 1.0.0
     */
    DIRECTIVE(isToken = false),

    /**
     * The anchor and the tag of a node.
     *
     * @since 1.0.0
     */
    PROPERTIES(isToken = false),

    /**
     * A plain, single-quoted or double-quoted scalar with its properties.
     *
     * @since 1.0.0
     */
    SCALAR(isToken = false),

    /**
     * A literal or folded block scalar with its properties.
     *
     * @since 1.0.0
     */
    BLOCK_SCALAR(isToken = false),

    /**
     * A node that refers to an anchored node through an alias.
     *
     * @since 1.0.0
     */
    ALIAS_NODE(isToken = false),

    /**
     * A node without content, such as the missing value of `key:`; it covers no text.
     *
     * @since 1.0.0
     */
    EMPTY_NODE(isToken = false),

    /**
     * A block sequence: entries introduced by `-` on lines of their own.
     *
     * @since 1.0.0
     */
    BLOCK_SEQUENCE(isToken = false),

    /**
     * An entry of a block sequence with its `-`.
     *
     * @since 1.0.0
     */
    BLOCK_SEQUENCE_ENTRY(isToken = false),

    /**
     * A block mapping: keys and values on lines of their own.
     *
     * @since 1.0.0
     */
    BLOCK_MAPPING(isToken = false),

    /**
     * An entry of a block mapping with its key and value.
     *
     * @since 1.0.0
     */
    BLOCK_MAPPING_ENTRY(isToken = false),

    /**
     * A flow sequence in `[` and `]`.
     *
     * @since 1.0.0
     */
    FLOW_SEQUENCE(isToken = false),

    /**
     * A flow mapping in `{` and `}`.
     *
     * @since 1.0.0
     */
    FLOW_MAPPING(isToken = false),

    /**
     * An entry of a flow mapping with its key and value.
     *
     * @since 1.0.0
     */
    FLOW_MAPPING_ENTRY(isToken = false),

    /**
     * A single pair inside a flow sequence, such as `[a: 1]`, which stands for a mapping of one entry.
     *
     * @since 1.0.0
     */
    FLOW_PAIR(isToken = false),

    /**
     * The key of a mapping entry or of a flow pair, with the whitespace around it.
     *
     * @since 1.0.0
     */
    KEY(isToken = false),

    /**
     * The value of a mapping entry or of a flow pair, with the whitespace and comments around it; the `:` before it
     * belongs to the entry.
     *
     * @since 1.0.0
     */
    VALUE(isToken = false),

    /**
     * Text the parser could not read as part of the grammar; a diagnostic of the tree describes it.
     *
     * @since 1.0.0
     */
    ERROR(isToken = false),
    ;

    /**
     * Whether nodes of this kind stand for a value of the document: a scalar, an alias, an empty node or a collection.
     *
     * @since 1.0.0
     */
    public val isValueNode: Boolean get() = this in valueNodes

    private companion object {
        private val valueNodes: Set<YamlSyntaxKind> = setOf(
            SCALAR,
            BLOCK_SCALAR,
            ALIAS_NODE,
            EMPTY_NODE,
            BLOCK_SEQUENCE,
            BLOCK_MAPPING,
            FLOW_SEQUENCE,
            FLOW_MAPPING,
        )
    }
}
