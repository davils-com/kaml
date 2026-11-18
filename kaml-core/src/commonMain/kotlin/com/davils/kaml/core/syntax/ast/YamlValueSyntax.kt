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

import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.YamlSyntaxNode
import com.davils.kaml.core.syntax.YamlSyntaxToken
import com.davils.kaml.core.syntax.internal.YamlSyntaxSupport

/**
 * The typed view of a value of a document: a scalar, an alias, an empty value, a sequence or a mapping.
 *
 * The view reads the syntax tree as it is written: it neither resolves tags nor follows aliases, and the values of
 * its collections are views as well. It is immutable and safe to share between threads. Scalars, anchors and tags
 * come from the input unchanged and must be escaped before they are shown.
 *
 * @since 1.0.0
 */
public sealed interface YamlValueSyntax {
    /**
     * The syntax node of the value; for an [Empty] value, the node that would hold it.
     *
     * @since 1.0.0
     */
    public val node: YamlSyntaxNode

    /**
     * The name of the anchor of the value without its `&`, or `null` if the value has none.
     *
     * @since 1.0.0
     */
    public val anchor: String? get() = propertyText(YamlSyntaxKind.ANCHOR)?.drop(1)

    /**
     * The tag of the value as it is written, such as `!!str` or `!local`, or `null` if the value has none; tag
     * handles are not resolved.
     *
     * @since 1.0.0
     */
    public val tag: String? get() = propertyText(YamlSyntaxKind.TAG)

    private fun propertyText(kind: YamlSyntaxKind): String? {
        val properties = node.childNodes.filter { it.kind == YamlSyntaxKind.PROPERTIES }
        return properties.firstNotNullOfOrNull { it.token(kind)?.text }
    }

    /**
     * A scalar in any of the five styles of YAML.
     *
     * @since 1.0.0
     */
    public class Scalar internal constructor(
        override val node: YamlSyntaxNode,
        private val parentIndent: Int,
    ) : YamlValueSyntax {
        /**
         * The style the scalar is written in.
         *
         * @since 1.0.0
         */
        public val style: YamlScalarStyle
            get() {
                val header = node.token(
                    YamlSyntaxKind.BLOCK_SCALAR_HEADER,
                ) ?: return styles.getValue(flowToken().kind)
                return YamlScalarStyle.Literal.takeIf { header.text.startsWith("|") } ?: YamlScalarStyle.Folded
            }

        /**
         * The content of the scalar, with quotes removed, escapes decoded and lines folded and chomped as YAML
         * defines; it is computed each time it is asked for.
         *
         * @since 1.0.0
         */
        public val value: String
            get() {
                val header = node.token(YamlSyntaxKind.BLOCK_SCALAR_HEADER) ?: return flowValue()
                val content = node.token(YamlSyntaxKind.BLOCK_SCALAR_CONTENT)
                return YamlSyntaxSupport.blockScalarValue(
                    header = header.text,
                    content = content?.text.orEmpty(),
                    parentIndent = parentIndent,
                )
            }

        private fun flowToken(): YamlSyntaxToken = node.tokens.first { it.kind in flowStyles }

        private fun flowValue(): String {
            val token = flowToken()
            return YamlSyntaxSupport.flowScalarValue(token.kind, token.text)
        }
    }

    /**
     * An alias that refers to an anchored value, such as `*base`.
     *
     * @since 1.0.0
     */
    public class Alias internal constructor(override val node: YamlSyntaxNode) : YamlValueSyntax {
        /**
         * The name of the anchor the alias refers to, without its `*`.
         *
         * @since 1.0.0
         */
        public val name: String
            get() {
                val alias = node.tokens.first { it.kind == YamlSyntaxKind.ALIAS }
                return alias.text.drop(1)
            }
    }

    /**
     * A value that is not written at all, such as the value of `key:` or the root of an empty document.
     *
     * @since 1.0.0
     */
    public class Empty internal constructor(host: YamlSyntaxNode) : YamlValueSyntax {
        override val node: YamlSyntaxNode = host
    }

    /**
     * A block or flow sequence.
     *
     * @since 1.0.0
     */
    public class Sequence internal constructor(
        override val node: YamlSyntaxNode,
        private val index: CommentIndex,
    ) : YamlValueSyntax {
        /**
         * Whether the sequence is written in flow style, in `[` and `]`.
         *
         * @since 1.0.0
         */
        public val isFlow: Boolean get() = node.kind == YamlSyntaxKind.FLOW_SEQUENCE

        /**
         * The items of the sequence in order; a pair inside a flow sequence, such as `[a: 1]`, is a [Mapping].
         *
         * @since 1.0.0
         */
        public val items: List<YamlValueSyntax> by lazy { SyntaxValues.items(node, index) }
    }

    /**
     * A block mapping, a flow mapping or a single pair inside a flow sequence.
     *
     * @since 1.0.0
     */
    public class Mapping internal constructor(
        override val node: YamlSyntaxNode,
        private val index: CommentIndex,
    ) : YamlValueSyntax {
        /**
         * Whether the mapping is written in flow style, in `{` and `}` or as a pair inside a flow sequence.
         *
         * @since 1.0.0
         */
        public val isFlow: Boolean get() = node.kind != YamlSyntaxKind.BLOCK_MAPPING

        /**
         * The entries of the mapping in the order of the text, duplicate keys included.
         *
         * @since 1.0.0
         */
        public val entries: List<YamlEntrySyntax> by lazy { SyntaxValues.entries(node, index) }

        /**
         * Returns the first entry whose scalar key has the text [key].
         *
         * @param key the text of the key to look for, compared with [YamlEntrySyntax.keyText].
         * @return the first entry with that key, or `null` if the mapping has none.
         * @since 1.0.0
         */
        public fun entry(key: String): YamlEntrySyntax? = entries.firstOrNull { it.keyText == key }
    }

    private companion object {
        val flowStyles: Set<YamlSyntaxKind> = setOf(
            YamlSyntaxKind.PLAIN_SCALAR,
            YamlSyntaxKind.SINGLE_QUOTED_SCALAR,
            YamlSyntaxKind.DOUBLE_QUOTED_SCALAR,
        )

        val styles: Map<YamlSyntaxKind, YamlScalarStyle> = mapOf(
            YamlSyntaxKind.PLAIN_SCALAR to YamlScalarStyle.Plain,
            YamlSyntaxKind.SINGLE_QUOTED_SCALAR to YamlScalarStyle.SingleQuoted,
            YamlSyntaxKind.DOUBLE_QUOTED_SCALAR to YamlScalarStyle.DoubleQuoted,
        )
    }
}
