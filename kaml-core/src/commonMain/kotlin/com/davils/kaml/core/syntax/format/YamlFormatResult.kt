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

import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kore.core.text.TextEdit

/**
 * The result of a [YamlFormatter]: the formatted tree, or the reason the formatter refused.
 *
 * @since 1.0.0
 */
public sealed interface YamlFormatResult {
    /**
     * A tree that was formatted; [edits] is empty when the tree was formatted already.
     *
     * @param edits the text edits that turn the text of the original tree into the text of [tree].
     * @property tree the tree of the formatted text, which composes to the same nodes as the original tree.
     * @since 1.0.0
     */
    public class Formatted(edits: List<TextEdit>, public val tree: YamlSyntaxTree) : YamlFormatResult {
        /**
         * The text edits that turn the text of the original tree into the text of [tree], ordered by their start; a
         * read-only copy.
         *
         * @since 1.0.0
         */
        public val edits: List<TextEdit> = buildList { addAll(edits) }

        override fun equals(other: Any?): Boolean = other is Formatted && edits == other.edits && tree == other.tree

        override fun hashCode(): Int = HASH_FACTOR * edits.hashCode() + tree.hashCode()

        override fun toString(): String = "Formatted(${edits.size} edits, $tree)"
    }

    /**
     * A tree the formatter refused to format.
     *
     * @param reason why the formatter refused.
     * @since 1.0.0
     */
    public class Refused(reason: YamlFormatRefusal) : YamlFormatResult {
        /**
         * Why the formatter refused.
         *
         * @since 1.0.0
         */
        public val reason: YamlFormatRefusal = reason

        override fun equals(other: Any?): Boolean = other is Refused && reason == other.reason

        override fun hashCode(): Int = reason.ordinal

        override fun toString(): String = "Refused($reason)"
    }

    private companion object {
        const val HASH_FACTOR: Int = 31
    }
}
