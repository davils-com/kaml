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

private const val HASH_FACTOR: Int = 31

/**
 * The result of reading a JSON Patch from its notation: the patch, or the first operation that is not valid.
 *
 * @since 1.0.0
 */
public sealed interface JsonPatchReading {
    /**
     * Every operation is valid.
     *
     * @param patch the patch that was read.
     * @since 1.0.0
     */
    public class Read(patch: JsonPatch) : JsonPatchReading {
        /**
         * The patch that was read.
         *
         * @since 1.0.0
         */
        public val patch: JsonPatch = patch

        /**
         * Returns whether [other] read an equal patch.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is a [Read] with an equal [patch].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Read && patch == other.patch

        /**
         * Returns the hash code of the patch.
         *
         * @return the hash code of [patch].
         * @since 1.0.0
         */
        override fun hashCode(): Int = patch.hashCode()

        /**
         * Returns a description of the patch.
         *
         * @return a description such as `Read(JsonPatch(2 operations))`.
         * @since 1.0.0
         */
        override fun toString(): String = "Read($patch)"
    }

    /**
     * The notation is not a valid patch.
     *
     * @property index the index of the first operation that is not valid, from zero, or `-1` when the patch as a
     * whole is not valid, such as when it is not a sequence or has more operations than
     * [com.davils.kaml.core.limit.YamlLimits.maximumPatchOperations].
     * @param reason why it is not valid, in constant words that never repeat the input.
     * @since 1.0.0
     */
    public class Invalid(public val index: Int, reason: String) : JsonPatchReading {
        /**
         * Why it is not valid, in constant words that never repeat the input.
         *
         * @since 1.0.0
         */
        public val reason: String = reason

        /**
         * Returns whether [other] refuses the same operation for the same reason.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is an [Invalid] with an equal [index] and [reason].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Invalid && index == other.index && reason == other.reason

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
         * @return a description such as `Invalid(0, an operation needs an "op")`.
         * @since 1.0.0
         */
        override fun toString(): String = "Invalid($index, $reason)"
    }
}
