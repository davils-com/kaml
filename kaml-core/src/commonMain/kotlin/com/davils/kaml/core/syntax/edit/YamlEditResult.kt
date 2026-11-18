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

package com.davils.kaml.core.syntax.edit

import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kore.core.text.TextEdit

/**
 * The result of an edit of a [YamlEditor]: the edited tree, or the reason the edit was refused.
 *
 * @since 1.0.0
 */
public sealed interface YamlEditResult {
    /**
     * An edit that was applied.
     *
     * The [tree] carries the diagnostics of the edited text: an edit never removes the errors a document already
     * had, and an edited text longer than `YamlLimits.maximumInputLength` is not parsed.
     *
     * @param edits the text edits that turn the text of the original tree into the text of [tree].
     * @property tree the tree of the edited text, parsed incrementally with the configuration of the original tree.
     * @since 1.0.0
     */
    public class Edited(edits: List<TextEdit>, public val tree: YamlSyntaxTree) : YamlEditResult {
        /**
         * The text edits that turn the text of the original tree into the text of [tree]; a read-only copy, ordered
         * as the editor made them.
         *
         * @since 1.0.0
         */
        public val edits: List<TextEdit> = buildList { addAll(edits) }

        override fun equals(other: Any?): Boolean = other is Edited && edits == other.edits && tree == other.tree

        override fun hashCode(): Int = HASH_FACTOR * edits.hashCode() + tree.hashCode()

        override fun toString(): String = "Edited(${edits.size} edits, $tree)"
    }

    /**
     * An edit that was refused.
     *
     * @param reason why the edit was refused.
     * @property path the path the edit was asked for.
     * @since 1.0.0
     */
    public class Refused(reason: YamlEditRefusal, public val path: YamlPath) : YamlEditResult {
        /**
         * Why the edit was refused.
         *
         * @since 1.0.0
         */
        public val reason: YamlEditRefusal = reason

        override fun equals(other: Any?): Boolean = other is Refused && reason == other.reason && path == other.path

        override fun hashCode(): Int = HASH_FACTOR * reason.ordinal + path.hashCode()

        override fun toString(): String = "Refused($reason at $path)"
    }

    private companion object {
        const val HASH_FACTOR: Int = 31
    }
}
