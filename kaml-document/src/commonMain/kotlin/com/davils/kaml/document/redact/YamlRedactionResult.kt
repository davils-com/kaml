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

package com.davils.kaml.document.redact

import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kore.core.text.TextEdit

/**
 * The result of redacting a syntax tree: the redacted tree with the text edits that redact it, or why the redaction
 * refused it.
 *
 * @since 1.0.0
 */
public sealed interface YamlRedactionResult {
    /**
     * Every target was redacted in place.
     *
     * @param edits the edits that turn the original text into that of [tree]; the list is copied.
     * @property tree the redacted syntax tree, which composes without errors.
     * @since 1.0.0
     */
    public class Redacted(edits: List<TextEdit>, public val tree: YamlSyntaxTree) : YamlRedactionResult {
        /**
         * The edits that turn the original text into that of [tree], in the order of the text, which cannot be
         * changed; empty if nothing was secret.
         *
         * @since 1.0.0
         */
        public val edits: List<TextEdit> = buildList { addAll(edits) }

        /**
         * Returns the number of edits, never the text.
         *
         * @return a description such as `Redacted(3 edits)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Redacted(${edits.size} edits)"
    }

    /**
     * The tree was refused and nothing was redacted.
     *
     * @param reason why the tree was refused.
     * @since 1.0.0
     */
    public class Refused(reason: YamlRedactionRefusal) : YamlRedactionResult {
        /**
         * Why the tree was refused.
         *
         * @since 1.0.0
         */
        public val reason: YamlRedactionRefusal = reason

        /**
         * Returns whether [other] refuses for the same reason.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is a [Refused] with an equal [reason].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Refused && reason == other.reason

        /**
         * Returns the hash code of the reason.
         *
         * @return the hash code of [reason].
         * @since 1.0.0
         */
        override fun hashCode(): Int = reason.hashCode()

        /**
         * Returns the reason.
         *
         * @return a description such as `Refused(InvalidDocument)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Refused($reason)"
    }
}
