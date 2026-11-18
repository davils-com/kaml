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

package com.davils.kaml.document.patch

import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kore.core.text.TextEdit

private const val HASH_FACTOR: Int = 31

/**
 * The result of applying a [JsonPatch] or a [JsonMergePatch] to a syntax tree: the edited tree with the text edits
 * that turn the old text into the new one, or the first change that failed.
 *
 * The edits keep the comments, the styles and the layout of everything the patch does not change.
 *
 * @since 1.0.0
 */
public sealed interface JsonPatchEdit {
    /**
     * Every change was applied in place.
     *
     * @param edits the edits that turn the text of the original tree into the text of [tree]; the list is copied.
     * @property tree the edited syntax tree.
     * @since 1.0.0
     */
    public class Edited(edits: List<TextEdit>, public val tree: YamlSyntaxTree) : JsonPatchEdit {
        /**
         * The edits that turn the text of the original tree into the text of [tree], which cannot be changed; empty
         * if nothing changed.
         *
         * @since 1.0.0
         */
        public val edits: List<TextEdit> = buildList { addAll(edits) }

        /**
         * Returns whether [other] is a result with the same edits and the same text.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is an [Edited] with equal [edits] and an equal text of its [tree].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean =
            other is Edited && edits == other.edits && tree.text == other.tree.text

        /**
         * Returns a hash code of the edits and the text.
         *
         * @return the combined hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * edits.hashCode() + tree.text.hashCode()

        /**
         * Returns the number of edits, never the text, which may be sensitive.
         *
         * @return a description such as `Edited(2 edits)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Edited(${edits.size} edits)"
    }

    /**
     * A change failed, so the tree was not edited.
     *
     * @property index the index of the operation that failed, from zero; `0` for a merge patch.
     * @param reason why it failed.
     * @since 1.0.0
     */
    public class Failed(public val index: Int, reason: JsonPatchRefusal) : JsonPatchEdit {
        /**
         * Why it failed.
         *
         * @since 1.0.0
         */
        public val reason: JsonPatchRefusal = reason

        /**
         * Returns whether [other] is a failure of the same change for the same reason.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is a [Failed] with an equal [index] and [reason].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Failed && index == other.index && reason == other.reason

        /**
         * Returns a hash code of the index and the reason.
         *
         * @return the combined hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * index + reason.hashCode()

        /**
         * Returns the index and the reason.
         *
         * @return a description such as `Failed(0, Unsupported)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Failed($index, $reason)"
    }
}
