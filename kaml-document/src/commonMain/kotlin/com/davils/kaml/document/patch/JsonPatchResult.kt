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

/**
 * The result of applying a [JsonPatch] to a node graph: the patched graph, or the first operation that failed.
 *
 * A patch is atomic: when an operation fails, no change of the operations before it is kept.
 *
 * @since 1.0.0
 */
public sealed interface JsonPatchResult {
    /**
     * Every operation was applied.
     *
     * @param node the root of the patched graph; the original graph is unchanged and shares the nodes the patch
     * did not touch.
     * @since 1.0.0
     */
    public class Applied(node: YamlNode) : JsonPatchResult {
        /**
         * The root of the patched graph; the original graph is unchanged and shares the nodes the patch did not touch.
         *
         * @since 1.0.0
         */
        public val node: YamlNode = node

        /**
         * Returns whether [other] is a result with an equal graph.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is an [Applied] with an equal [node].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Applied && node == other.node

        /**
         * Returns the hash code of the graph.
         *
         * @return the hash code of [node].
         * @since 1.0.0
         */
        override fun hashCode(): Int = node.hashCode()

        /**
         * Returns the kind of the patched root, never its content.
         *
         * @return a description such as `Applied(Mapping)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Applied(${node.kind})"
    }

    /**
     * An operation failed, so nothing was changed.
     *
     * @property index the index of the operation that failed, from zero.
     * @param reason why it failed.
     * @since 1.0.0
     */
    public class Failed(public val index: Int, reason: JsonPatchRefusal) : JsonPatchResult {
        /**
         * Why it failed.
         *
         * @since 1.0.0
         */
        public val reason: JsonPatchRefusal = reason

        /**
         * Returns whether [other] is a failure of the same operation for the same reason.
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
         * @return a description such as `Failed(2, TestFailed)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Failed($index, $reason)"

        private companion object {
            const val HASH_FACTOR: Int = 31
        }
    }
}
