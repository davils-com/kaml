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

package com.davils.kaml.serialization.json

import com.davils.kaml.core.node.YamlNode
import kotlinx.serialization.json.JsonElement

/**
 * The outcome of converting a [YamlNode] to a [JsonElement] with [toJsonElement].
 *
 * @since 1.0.0
 */
public sealed interface YamlJsonConversion {
    /**
     * The node converted to [element].
     *
     * @since 1.0.0
     */
    public class Converted private constructor(
        /**
         * The JSON element that holds the same data as the node.
         *
         * @since 1.0.0
         */
        public val element: JsonElement,
    ) : YamlJsonConversion {
        override fun equals(other: Any?): Boolean = other is Converted && element == other.element

        override fun hashCode(): Int = element.hashCode()

        override fun toString(): String = "Converted"

        internal companion object {
            fun of(element: JsonElement): Converted = Converted(element)
        }
    }

    /**
     * The conversion was refused at [node] for [reason].
     *
     * @since 1.0.0
     */
    public class Refused private constructor(
        /**
         * The node that JSON cannot hold.
         *
         * @since 1.0.0
         */
        public val node: YamlNode,
        /**
         * Why JSON cannot hold [node]: constant text without input, safe to show.
         *
         * @since 1.0.0
         */
        public val reason: String,
    ) : YamlJsonConversion {
        override fun equals(other: Any?): Boolean = other is Refused && node == other.node && reason == other.reason

        override fun hashCode(): Int = HASH_FACTOR * node.hashCode() + reason.hashCode()

        override fun toString(): String = "Refused(reason=$reason)"

        internal companion object {
            private const val HASH_FACTOR: Int = 31

            fun of(node: YamlNode, reason: String): Refused = Refused(node, reason)
        }
    }
}
