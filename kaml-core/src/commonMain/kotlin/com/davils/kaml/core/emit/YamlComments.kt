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

package com.davils.kaml.core.emit

import com.davils.kaml.core.node.NodeIdentity
import com.davils.kaml.core.node.YamlNode

/**
 * The comments that `Yaml.emit(document, comments)` writes into a document, line by line, keyed by node.
 *
 * A node is found by identity, not by equality: two equal nodes that are distinct objects have comments of their own.
 * The emitter writes the comments of the root of a document above the document, below its `---` marker when it has
 * one, and the comments of a key of a block mapping above the entry of that key, at the indentation of the key. The
 * comments of other nodes, such as items of a sequence, values and keys of a flow mapping, are not written.
 *
 * Instances are built with [yamlComments] or [YamlCommentsBuilder], which split every comment into lines and refuse a
 * line that could break the YAML or hide text from a reader, so every line held here is safe to write. The
 * constructor checks every line again and copies the lines, so not even a Java caller, which can reach it, holds an
 * instance with a line that could break the YAML. They are immutable and safe to share between threads; the lists
 * they return cannot be changed, not even through a cast.
 *
 * @since 1.0.0
 */
public class YamlComments internal constructor(byNode: Map<NodeIdentity, List<String>>) {
    private val byNode: Map<NodeIdentity, List<String>> = byNode.mapValues { (_, lines) -> CommentLines.checked(lines) }

    /**
     * Whether no node has a comment.
     *
     * @since 1.0.0
     */
    public val isEmpty: Boolean get() = byNode.isEmpty()

    /**
     * Returns the comment lines of [node], without the `#` that starts each of them when it is written.
     *
     * @param node the node whose comments to return, compared by identity.
     * @return the lines of every comment added for [node], in the order they were added; empty if it has none. The
     * list cannot be changed.
     * @since 1.0.0
     */
    public fun of(node: YamlNode): List<String> = byNode[NodeIdentity(node)].orEmpty()

    override fun toString(): String = "YamlComments(${byNode.size} nodes)"

    /**
     * Holds the comments that mark nothing.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The comments of no node, which the emitter uses when it is given none.
         *
         * @since 1.0.0
         */
        public val None: YamlComments = YamlComments(emptyMap())
    }
}
