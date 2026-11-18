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
package com.davils.kaml.core.node

import com.davils.kaml.core.model.YamlTagDirective
import com.davils.kaml.core.model.YamlVersion
import com.davils.kore.core.text.TextRange

private const val NONE: String = "none"

private fun shown(value: Any?): String {
    if (value == null) return NONE
    return value.toString()
}

/**
 * A document of a YAML stream: its root node and the directives and markers around it.
 *
 * A document is immutable and safe to share between threads; two documents are equal when their roots and every
 * other property are equal.
 *
 * @property root the root node of the document.
 * @property version the version of the `%YAML` directive of the document, or `null` without one.
 * @param tagDirectives the `%TAG` directives of the document; the document keeps a copy.
 * @property isExplicitStart whether the document starts with the marker `---`.
 * @property isExplicitEnd whether the document ends with the marker `...`.
 * @property range the place of the document in its source, or `null` for a document built in code.
 * @since 1.0.0
 */
public class YamlDocument(
    public val root: YamlNode,
    public val version: YamlVersion? = null,
    tagDirectives: List<YamlTagDirective> = emptyList(),
    public val isExplicitStart: Boolean = false,
    public val isExplicitEnd: Boolean = false,
    public val range: TextRange? = null,
) {
    /**
     * The `%TAG` directives of the document, in order.
     *
     * @since 1.0.0
     */
    public val tagDirectives: List<YamlTagDirective> = buildList { addAll(tagDirectives) }

    private val properties: List<Any?>
        get() = listOf(root, version, tagDirectives, isExplicitStart, isExplicitEnd, range)

    /**
     * Returns whether [other] is a document with an equal root and equal other properties.
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is an equal document, `false` otherwise.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is YamlDocument && properties == other.properties

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code of every property.
     * @since 1.0.0
     */
    override fun hashCode(): Int = properties.hashCode()

    /**
     * Returns the root in its bounded form and the other properties, with the number of tag directives.
     *
     * @return a text such as `YamlDocument(root=Mapping(…), version=null, tagDirectives=0, …)`.
     * @since 1.0.0
     */
    override fun toString(): String =
        "YamlDocument(root=$root, version=${shown(version)}, tagDirectives=${tagDirectives.size}, " +
            "isExplicitStart=$isExplicitStart, isExplicitEnd=$isExplicitEnd, range=${shown(range)})"
}
