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

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.diff.GraphDiff
import com.davils.kaml.document.patch.graph.GraphPatching
import com.davils.kaml.document.patch.notation.PatchNotation
import com.davils.kaml.document.patch.syntax.SyntaxPatching

/**
 * A JSON Patch as RFC 6902 defines it: a list of operations applied in order, to a node graph or in place to a
 * lossless syntax tree.
 *
 * A patch is atomic: it is applied completely or not at all. A pointer resolves a duplicate key to its first
 * occurrence, and a replacement or removal acts on that occurrence only. Every application runs under the [limits]
 * of the patch: the values it adds, copies included, and the width of every mapping and sequence an operation looks
 * into or rebuilds count against [YamlLimits.maximumNodes], the result must not be deeper than
 * [YamlLimits.maximumDepth], and every walk is iterative or bounded by that depth.
 *
 * A patch is immutable and safe to share between threads.
 *
 * @param operations the operations in the order they apply; the list is copied.
 * @property limits the limits every application of the patch runs under.
 * @throws IllegalArgumentException if there are more operations than [YamlLimits.maximumPatchOperations].
 * @since 1.0.0
 */
public class JsonPatch(
    operations: List<JsonPatchOperation>,
    public val limits: YamlLimits = YamlLimits.Untrusted,
) {
    /**
     * The operations in the order they apply, which cannot be changed.
     *
     * @since 1.0.0
     */
    public val operations: List<JsonPatchOperation> = buildList { addAll(operations) }

    init {
        require(this.operations.size <= limits.maximumPatchOperations) {
            "A JSON patch may hold at most ${limits.maximumPatchOperations} operations, but held " +
                "${this.operations.size}"
        }
    }

    /**
     * Applies this patch to the graph [node], which stays unchanged.
     *
     * @param node the root of the graph to patch.
     * @return the patched graph, or the first operation that failed and why.
     * @since 1.0.0
     */
    public fun apply(node: YamlNode): JsonPatchResult = GraphPatching(limits).apply(operations, node)

    /**
     * Applies this patch in place to the document at [document] of [tree], keeping its comments, styles and layout.
     *
     * The document is composed once to check every operation on its graph, and the edited tree is composed once
     * more at the end: when it does not give the graph the patch gives, for example because a change goes through
     * an alias or a duplicate key, the patch fails with [JsonPatchRefusal.Unsupported] and nothing is changed.
     *
     * @param tree the syntax tree to edit, which stays unchanged.
     * @param document the index of the document in [tree], from zero.
     * @return the edited tree and its text edits, or the first operation that failed and why.
     * @since 1.0.0
     */
    public fun apply(tree: YamlSyntaxTree, document: Int = 0): JsonPatchEdit =
        SyntaxPatching(tree, document, limits).apply(operations)

    /**
     * Writes this patch in its notation: a sequence of mappings with the members `op`, `path`, `from` and `value`.
     *
     * @return the notation, which [read] reads back to an equal patch.
     * @since 1.0.0
     */
    public fun toNode(): YamlNode = PatchNotation.write(operations)

    /**
     * Returns whether [other] is a patch with equal operations.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is a [JsonPatch] with equal [operations].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is JsonPatch && operations == other.operations

    /**
     * Returns the hash code of the operations.
     *
     * @return the hash code of [operations].
     * @since 1.0.0
     */
    override fun hashCode(): Int = operations.hashCode()

    /**
     * Returns the number of operations, never their values, which may be sensitive.
     *
     * @return a description such as `JsonPatch(2 operations)`.
     * @since 1.0.0
     */
    override fun toString(): String = "JsonPatch(${operations.size} operations)"

    /**
     * Reads and computes JSON patches.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads a patch from its notation: a sequence of mappings, each with an `op` and the members its operation
         * needs.
         *
         * Members an operation does not define are ignored, as RFC 6902 section 4 requires. Pointers are parsed as
         * RFC 6901 defines them.
         *
         * @param node the notation, such as the composed graph of a YAML or JSON document.
         * @param limits the limits of the patch; [YamlLimits.Untrusted] by default.
         * @return the patch, or the first operation that is not valid; a sequence with more operations than
         * [YamlLimits.maximumPatchOperations] is refused before any of it is read.
         * @since 1.0.0
         */
        public fun read(node: YamlNode, limits: YamlLimits = YamlLimits.Untrusted): JsonPatchReading =
            PatchNotation.read(node, limits)

        /**
         * Computes a patch that turns [source] into [target].
         *
         * Mappings whose keys are all distinct strings are compared member by member, sequences item by item, and
         * every other difference, such as a different tag, replaces the whole node.
         *
         * @param source the graph the patch applies to.
         * @param target the graph the patch gives.
         * @param limits the limits of the walk and of the patch; [YamlLimits.Untrusted] by default.
         * @return a patch that turns [source] into [target]; empty if they are equal.
         * @throws YamlDocumentLimitException if the walk visits more nodes than [YamlLimits.maximumNodes], descends
         * deeper than [YamlLimits.maximumDepth] or yields more operations than [YamlLimits.maximumPatchOperations].
         * @since 1.0.0
         */
        public fun diff(source: YamlNode, target: YamlNode, limits: YamlLimits = YamlLimits.Untrusted): JsonPatch =
            JsonPatch(GraphDiff(limits).operations(source, target), limits)
    }
}
