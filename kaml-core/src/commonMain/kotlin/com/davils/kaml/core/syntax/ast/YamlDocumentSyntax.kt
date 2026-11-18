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
import com.davils.kaml.core.syntax.YamlSyntaxTree

/**
 * The typed view of a document of a [YamlSyntaxTree]: its markers, its directives and its root value.
 *
 * The view reads the syntax tree as it is, without composing it: it resolves no tags, aliases or merge keys and
 * reports no diagnostics, so it also shows documents with errors as far as they could be parsed. It is immutable and
 * safe to share between threads. Everything it returns comes from the input unchanged and must be escaped before it
 * is shown.
 *
 * @property node the syntax node of the document, of kind [YamlSyntaxKind.DOCUMENT].
 * @since 1.0.0
 */
public class YamlDocumentSyntax internal constructor(
    public val node: YamlSyntaxNode,
    private val index: CommentIndex,
) {
    /**
     * Whether the document starts with the marker `---`.
     *
     * @since 1.0.0
     */
    public val hasExplicitStart: Boolean get() = node.token(YamlSyntaxKind.DOCUMENT_START) != null

    /**
     * Whether the document ends with the marker `...`.
     *
     * @since 1.0.0
     */
    public val hasExplicitEnd: Boolean get() = node.token(YamlSyntaxKind.DOCUMENT_END) != null

    /**
     * The directives before the document, such as `%YAML 1.2`, as nodes of kind [YamlSyntaxKind.DIRECTIVE].
     *
     * @since 1.0.0
     */
    public val directives: List<YamlSyntaxNode>
        get() = node.childNodes.filter { child -> child.kind == YamlSyntaxKind.DIRECTIVE }

    /**
     * The root value of the document; [YamlValueSyntax.Empty] for a document without content.
     *
     * @since 1.0.0
     */
    public val root: YamlValueSyntax by lazy { SyntaxValues.wrapped(node, ROOT_INDENT, index) }

    private companion object {
        const val ROOT_INDENT: Int = -1
    }
}
