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

package com.davils.kaml.document.merge

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * How a merge combines a sequence of a later layer with the sequence of the layers before it.
 *
 * @since 1.0.0
 */
public sealed interface YamlSequenceMerge {
    /**
     * The later sequence replaces the earlier one.
     *
     * @since 1.0.0
     */
    public object Replace : YamlSequenceMerge {
        /**
         * Returns the name of the strategy.
         *
         * @return `Replace`.
         * @since 1.0.0
         */
        override fun toString(): String = "Replace"
    }

    /**
     * The items of the later sequence are appended to the earlier one.
     *
     * @since 1.0.0
     */
    public object Append : YamlSequenceMerge {
        /**
         * Returns the name of the strategy.
         *
         * @return `Append`.
         * @since 1.0.0
         */
        override fun toString(): String = "Append"
    }

    /**
     * The items are mappings identified by the text of their member [key]: a later item merges into the earlier item
     * with the same identity, the first one when several share it, and is appended when there is none. An item
     * without the member is appended. The items are found through an ordered index of their identities, so the merge
     * takes time in proportion to the items times the logarithm of their number, not to their product, also when the
     * hash codes of the identities collide.
     *
     * @param key the name of the member that identifies an item; must not be blank.
     * @since 1.0.0
     */
    public class ByKey(key: String) : YamlSequenceMerge {
        /**
         * The name of the member that identifies an item; must not be blank.
         *
         * @since 1.0.0
         */
        public val key: String = key

        /**
         * Returns whether [other] merges by the same key.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is a [ByKey] with an equal [key].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is ByKey && key == other.key

        /**
         * Returns the hash code of the key.
         *
         * @return the hash code of [key].
         * @since 1.0.0
         */
        override fun hashCode(): Int = key.hashCode()

        /**
         * Returns the strategy with its key escaped.
         *
         * @return a description such as `ByKey("name")`.
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        override fun toString(): String = "ByKey(${SafeText.quoted(key)})"
    }
}
