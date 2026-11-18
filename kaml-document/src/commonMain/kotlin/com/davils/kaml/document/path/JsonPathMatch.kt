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

package com.davils.kaml.document.path

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.pointer.JsonPointer

/**
 * A node a [JsonPath] selected, with its location.
 *
 * @property location the normalized location of the node below the root of the query, as a JSON pointer.
 * @param node the selected node itself, not a copy.
 * @since 1.0.0
 */
public class JsonPathMatch(public val location: JsonPointer, node: YamlNode) {
    /**
     * The selected node itself, not a copy.
     *
     * @since 1.0.0
     */
    public val node: YamlNode = node

    /**
     * Returns whether [other] is a match of an equal node at the same location.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is a [JsonPathMatch] with an equal [location] and an equal [node].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is JsonPathMatch && location == other.location && node == other.node

    /**
     * Returns a hash code of the location and the node.
     *
     * @return the combined hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * location.hashCode() + node.hashCode()

    /**
     * Returns the escaped location and the kind of the node, never its content, which may be sensitive.
     *
     * @return a description such as `JsonPathMatch(/a/0, Scalar)`.
     * @since 1.0.0
     */
    override fun toString(): String = "JsonPathMatch($location, ${node.kind})"

    private companion object {
        const val HASH_FACTOR: Int = 31
    }
}
