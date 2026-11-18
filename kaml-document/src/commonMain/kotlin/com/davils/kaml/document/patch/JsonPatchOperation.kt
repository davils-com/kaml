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

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.pointer.JsonPointer

private const val HASH_FACTOR: Int = 31

/**
 * An operation of a JSON Patch, as RFC 6902 defines it.
 *
 * Every operation is immutable. Its [toString] shows its kind and its pointers escaped, never its value, which may be
 * sensitive.
 *
 * @since 1.0.0
 */
public sealed interface JsonPatchOperation {
    /**
     * The location the operation changes or tests.
     *
     * @since 1.0.0
     */
    public val path: JsonPointer

    /**
     * Adds [value] at [path]: inserts it into a sequence before the index, or at its end for `-`, and adds or
     * replaces the member of a mapping. Adding at the root replaces the whole document.
     *
     * @property path the location to add at.
     * @param value the value to add.
     * @since 1.0.0
     */
    public class Add(override val path: JsonPointer, value: YamlNode) : JsonPatchOperation {
        /**
         * The value to add.
         *
         * @since 1.0.0
         */
        public val value: YamlNode = value

        /**
         * Returns whether [other] adds an equal value at the same location.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is an [Add] with an equal [path] and [value].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Add && path == other.path && value == other.value

        /**
         * Returns a hash code of the location and the value.
         *
         * @return the combined hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * path.hashCode() + value.hashCode()

        /**
         * Returns the kind and the escaped location, never the value.
         *
         * @return a description such as `Add(/a/0)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Add($path)"
    }

    /**
     * Removes the node at [path].
     *
     * @param path the location to remove.
     * @since 1.0.0
     */
    public class Remove(path: JsonPointer) : JsonPatchOperation {
        override val path: JsonPointer = path

        /**
         * Returns whether [other] removes the same location.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is a [Remove] with an equal [path].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Remove && path == other.path

        /**
         * Returns the hash code of the location.
         *
         * @return the hash code of [path].
         * @since 1.0.0
         */
        override fun hashCode(): Int = path.hashCode()

        /**
         * Returns the kind and the escaped location.
         *
         * @return a description such as `Remove(/a)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Remove($path)"
    }

    /**
     * Replaces the node at [path], which must exist, with [value].
     *
     * @property path the location to replace.
     * @param value the new value.
     * @since 1.0.0
     */
    public class Replace(override val path: JsonPointer, value: YamlNode) : JsonPatchOperation {
        /**
         * The new value.
         *
         * @since 1.0.0
         */
        public val value: YamlNode = value

        /**
         * Returns whether [other] replaces the same location with an equal value.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is a [Replace] with an equal [path] and [value].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Replace && path == other.path && value == other.value

        /**
         * Returns a hash code of the location and the value.
         *
         * @return the combined hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * path.hashCode() + value.hashCode()

        /**
         * Returns the kind and the escaped location, never the value.
         *
         * @return a description such as `Replace(/a)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Replace($path)"
    }

    /**
     * Removes the node at [from] and adds it at [path], which must not lie below [from].
     *
     * @param from the location to move from.
     * @property path the location to move to.
     * @since 1.0.0
     */
    public class Move(from: JsonPointer, override val path: JsonPointer) : JsonPatchOperation {
        /**
         * The location to move from.
         *
         * @since 1.0.0
         */
        public val from: JsonPointer = from

        /**
         * Returns whether [other] moves between the same locations.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is a [Move] with an equal [from] and [path].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Move && from == other.from && path == other.path

        /**
         * Returns a hash code of both locations.
         *
         * @return the combined hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * from.hashCode() + path.hashCode()

        /**
         * Returns the kind and both escaped locations.
         *
         * @return a description such as `Move(/a -> /b)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Move($from -> $path)"
    }

    /**
     * Adds the node at [from] at [path] as well.
     *
     * The copy shares the nodes of its source, but counts with all of them against
     * [com.davils.kaml.core.limit.YamlLimits.maximumNodes], so repeated copies cannot grow a graph without bound.
     *
     * @param from the location to copy from.
     * @property path the location to copy to.
     * @since 1.0.0
     */
    public class Copy(from: JsonPointer, override val path: JsonPointer) : JsonPatchOperation {
        /**
         * The location to copy from.
         *
         * @since 1.0.0
         */
        public val from: JsonPointer = from

        /**
         * Returns whether [other] copies between the same locations.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is a [Copy] with an equal [from] and [path].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Copy && from == other.from && path == other.path

        /**
         * Returns a hash code of both locations.
         *
         * @return the combined hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * from.hashCode() + path.hashCode()

        /**
         * Returns the kind and both escaped locations.
         *
         * @return a description such as `Copy(/a -> /b)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Copy($from -> $path)"
    }

    /**
     * Tests that the node at [path] equals [value] in the sense of JSON: mappings by their members whatever their
     * order, sequences item by item, strings by their text and numbers by their value. Numbers compare exactly: an
     * integer by its canonical decimal, never through a `Double`, so `9007199254740993` differs from
     * `9007199254740992`, while `1` equals `1.0`.
     *
     * @property path the location to test.
     * @param value the value the node must equal.
     * @since 1.0.0
     */
    public class Test(override val path: JsonPointer, value: YamlNode) : JsonPatchOperation {
        /**
         * The value the node must equal.
         *
         * @since 1.0.0
         */
        public val value: YamlNode = value

        /**
         * Returns whether [other] tests the same location for an equal value.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is a [Test] with an equal [path] and [value].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Test && path == other.path && value == other.value

        /**
         * Returns a hash code of the location and the value.
         *
         * @return the combined hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * path.hashCode() + value.hashCode()

        /**
         * Returns the kind and the escaped location, never the value.
         *
         * @return a description such as `Test(/a)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Test($path)"
    }
}
