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

package com.davils.kaml.core.emit

import com.davils.kaml.core.YamlViolationCodes
import com.davils.kaml.core.model.YamlVersion
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * How the emitter of a [com.davils.kaml.core.Yaml] lays out the YAML it writes.
 *
 * The options change only the form of the output, never what it means: whatever they say, the emitter quotes a
 * scalar that would read as another type or break the structure, escapes every character that a reader could not
 * see or that YAML 1.1 reads as a line break, percent-encodes tags and replaces anchor names outside its alphabet,
 * so the output reads back as the same graph.
 *
 * Options are created with [YamlEmitOptionsBuilder], usually through the `emitter { }` block of the
 * [com.davils.kaml.core.yaml] DSL, and [Default] holds the defaults. They are validated when they are built: an
 * [indent] outside [MINIMUM_INDENT]..[MAXIMUM_INDENT] is reported with [YamlViolationCodes.INVALID_INDENT], and the
 * builder then fails with the `ValidationException` of Kore instead of creating the options.
 *
 * The options are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class YamlEmitOptions internal constructor(builder: YamlEmitOptionsBuilder) : Validatable {
    /**
     * The number of spaces that every level of a block collection is indented by, from [MINIMUM_INDENT] to
     * [MAXIMUM_INDENT]; the indentation indicator of a literal block scalar is a single digit, so no wider indent can
     * be written.
     *
     * @since 1.0.0
     */
    public val indent: Int = builder.indent

    /**
     * Whether a block sequence that is the value of a mapping entry is indented below its key, as in `key:\n  - a`,
     * rather than written at the column of its key, as in `key:\n- a`.
     *
     * @since 1.0.0
     */
    public val isSequenceIndented: Boolean = builder.isSequenceIndented

    /**
     * The line break that ends every line of the output, the lines of a literal block scalar included. A line break
     * inside a quoted scalar is always written as the escape `\n`.
     *
     * @since 1.0.0
     */
    public val lineBreak: YamlLineBreak = builder.lineBreak

    /**
     * Whether the entries of every mapping are written sorted by the text of their keys. The sort is stable, and a
     * key that is no scalar sorts like an empty text.
     *
     * @since 1.0.0
     */
    public val hasSortedKeys: Boolean = builder.hasSortedKeys

    /**
     * Whether every document starts with the marker `---`. Without it, the marker is written only where it is needed:
     * before a document with directives, before every document after the first and where the document asks for it.
     *
     * @since 1.0.0
     */
    public val isDocumentStartExplicit: Boolean = builder.isDocumentStartExplicit

    /**
     * The quotes a scalar is written in when it cannot be written plain and asks for no quote style of its own. A
     * scalar that holds a line break or a character that must be escaped is always written in double quotes, since
     * single quotes cannot escape anything.
     *
     * @since 1.0.0
     */
    public val quoteStyle: YamlQuoteStyle = builder.quoteStyle

    /**
     * The oldest YAML version the output must be read correctly by. [YamlVersion.V1_1] also quotes every plain scalar
     * that YAML 1.1 reads as another type, such as `yes`, `on`, `0755` or `1:30`; [YamlVersion.V1_2] quotes only what
     * YAML 1.2 would misread. The characters that YAML 1.1 reads as line breaks, `U+0085`, `U+2028` and `U+2029`, are
     * escaped in either case.
     *
     * @since 1.0.0
     */
    public val compatibility: YamlVersion = builder.compatibility

    /**
     * Whether the styles of the nodes are kept where they are safe: quoted and block scalars stay quoted or block, and
     * flow collections stay flow. Without it, every scalar is written plain where it can be and every collection in
     * block style.
     *
     * @since 1.0.0
     */
    public val isStylePreserved: Boolean = builder.isStylePreserved

    override fun ValidationScope.validate() {
        if (indent !in MINIMUM_INDENT..MAXIMUM_INDENT) {
            fail(
                ::indent,
                "must be between $MINIMUM_INDENT and $MAXIMUM_INDENT, but was $indent",
                YamlViolationCodes.INVALID_INDENT,
            )
        }
    }

    override fun toString(): String =
        "YamlEmitOptions(indent=$indent, isSequenceIndented=$isSequenceIndented, lineBreak=$lineBreak, " +
            "hasSortedKeys=$hasSortedKeys, isDocumentStartExplicit=$isDocumentStartExplicit, " +
            "quoteStyle=$quoteStyle, compatibility=$compatibility, isStylePreserved=$isStylePreserved)"

    /**
     * The bounds of [indent] and the default options.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The smallest [indent], one space.
         *
         * @since 1.0.0
         */
        public const val MINIMUM_INDENT: Int = 1

        /**
         * The largest [indent], nine spaces, the widest indentation indicator of a block scalar.
         *
         * @since 1.0.0
         */
        public const val MAXIMUM_INDENT: Int = 9

        /**
         * The default options: an indent of two spaces, indented sequences, [YamlLineBreak.Lf], keys in their order,
         * document markers only where needed, [YamlQuoteStyle.Double], output for YAML 1.2 readers and preserved
         * styles.
         *
         * @since 1.0.0
         */
        public val Default: YamlEmitOptions = YamlEmitOptionsBuilder().build()
    }
}
