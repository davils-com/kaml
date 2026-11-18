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

import com.davils.kaml.core.node.YamlNode
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * One layer of a merge: a named node graph, such as the defaults, the settings of a site or those of a host.
 *
 * @property name the name of the layer, which [YamlMerged.originOf] reports for the values it contributes.
 * @param node the root of the graph of the layer.
 * @since 1.0.0
 */
public class YamlLayer(public val name: String, node: YamlNode) {
    /**
     * The root of the graph of the layer.
     *
     * @since 1.0.0
     */
    public val node: YamlNode = node

    /**
     * Returns whether [other] is a layer with the same name and an equal graph.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is a [YamlLayer] with an equal [name] and [node].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is YamlLayer && name == other.name && node == other.node

    /**
     * Returns a hash code of the name and the graph.
     *
     * @return the combined hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * name.hashCode() + node.hashCode()

    /**
     * Returns the escaped name and the kind of the graph, never its content.
     *
     * @return a description such as `YamlLayer("defaults", Mapping)`.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String = "YamlLayer(${SafeText.quoted(name)}, ${node.kind})"

    private companion object {
        const val HASH_FACTOR: Int = 31
    }
}
