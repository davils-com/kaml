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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * The path from the root of a document to one of its values: a key for each mapping and an index for each sequence
 * on the way.
 *
 * Paths are built from [ROOT] with [key] and [index], such as `YamlPath.ROOT.key("server").index(0)`. They are
 * immutable values, compare equal when their segments are equal and are safe to share between threads. A path
 * addresses values by the text of scalar keys; keys that are no scalars cannot be addressed.
 *
 * @since 1.0.0
 */
public class YamlPath private constructor(segments: List<Segment>) {
    /**
     * The segments of the path from the root, in order; a read-only copy.
     *
     * @since 1.0.0
     */
    public val segments: List<Segment> = buildList { addAll(segments) }

    /**
     * The path without its last segment, or `null` for [ROOT].
     *
     * @since 1.0.0
     */
    public val parent: YamlPath? get() = YamlPath(segments.dropLast(1)).takeIf { segments.isNotEmpty() }

    /**
     * The last segment of the path, or `null` for [ROOT].
     *
     * @since 1.0.0
     */
    public val last: Segment? get() = segments.lastOrNull()

    /**
     * Returns this path extended by the mapping key [name].
     *
     * @param name the text of the key, which may be any string.
     * @return a new path that ends with the key [name].
     * @since 1.0.0
     */
    public fun key(name: String): YamlPath = YamlPath(segments + Segment.Key(name))

    /**
     * Returns this path extended by the sequence index [index].
     *
     * @param index the zero-based index of an item; an index outside the sequence addresses no value.
     * @return a new path that ends with the index [index].
     * @since 1.0.0
     */
    public fun index(index: Int): YamlPath = YamlPath(segments + Segment.Index(index))

    override fun equals(other: Any?): Boolean = other is YamlPath && segments == other.segments

    override fun hashCode(): Int = segments.hashCode()

    /**
     * Returns the path as `/` followed by its segments separated by `/`, with each key quoted and escaped, such as
     * `/"server"/0`.
     *
     * @return the escaped, readable form of the path.
     * @since 1.0.0
     */
    override fun toString(): String = "/" + segments.joinToString("/")

    /**
     * A segment of a [YamlPath].
     *
     * @since 1.0.0
     */
    public sealed interface Segment {
        /**
         * A segment that addresses the value of the entry of a mapping whose scalar key has the text [name].
         *
         * @property name the text of the key, which may be any string.
         * @since 1.0.0
         */
        public class Key(public val name: String) : Segment {
            override fun equals(other: Any?): Boolean = other is Key && name == other.name

            override fun hashCode(): Int = name.hashCode()

            /**
             * Returns the key in double quotes, with every character that could forge or hide text escaped.
             *
             * @return the quoted, escaped key.
             * @since 1.0.0
             */
            @OptIn(DavilsInternal::class)
            override fun toString(): String = SafeText.quoted(name)
        }

        /**
         * A segment that addresses the item at [index] of a sequence.
         *
         * @property index the zero-based index of the item.
         * @since 1.0.0
         */
        public class Index(public val index: Int) : Segment {
            override fun equals(other: Any?): Boolean = other is Index && index == other.index

            override fun hashCode(): Int = index

            override fun toString(): String = index.toString()
        }
    }

    /**
     * Holds the root path.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The empty path, which addresses the root value of a document.
         *
         * @since 1.0.0
         */
        public val ROOT: YamlPath = YamlPath(emptyList())
    }
}
