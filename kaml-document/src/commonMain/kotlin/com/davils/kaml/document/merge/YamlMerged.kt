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
import com.davils.kaml.document.merge.trace.OriginIndex
import com.davils.kaml.document.merge.trace.OriginMap
import com.davils.kaml.document.pointer.JsonPointer

/**
 * The result of a merge: the merged graph and the layer every part of it comes from.
 *
 * @property node the root of the merged graph.
 * @since 1.0.0
 */
public class YamlMerged internal constructor(public val node: YamlNode, origins: OriginMap) {
    /**
     * The name of the layer that last set each merged location, by its pointer; it cannot be changed. A location
     * inside a value that one layer contributed as a whole is not listed; [originOf] finds it through its ancestors.
     *
     * @since 1.0.0
     */
    public val origins: Map<JsonPointer, String> = origins

    private val index = OriginIndex(origins)

    /**
     * Returns the name of the layer the value at [pointer] comes from: that of [pointer] itself, or of its nearest
     * ancestor that [origins] lists.
     *
     * The pointer is walked once from the root, so the time grows with the depth of the merged graph at most, not
     * with the square of the length of [pointer].
     *
     * @param pointer the location in [node].
     * @return the name of the layer, or `null` if no ancestor of [pointer] is listed.
     * @since 1.0.0
     */
    public fun originOf(pointer: JsonPointer): String? = index.originOf(pointer)

    /**
     * Returns the kind of the merged graph and the number of origins, never the content, which may be sensitive.
     *
     * @return a description such as `YamlMerged(Mapping, 4 origins)`.
     * @since 1.0.0
     */
    override fun toString(): String = "YamlMerged(${node.kind}, ${origins.size} origins)"
}
