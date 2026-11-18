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
import com.davils.kaml.document.patch.merge.MergePatchEditing
import com.davils.kaml.document.patch.merge.MergePatching

/**
 * A JSON Merge Patch as RFC 7386 defines it: a mapping whose members replace, merge into or, when `null`, remove the
 * members of the target, applied to a node graph or in place to a lossless syntax tree.
 *
 * Members are matched by the text of their keys; a duplicate key of the target resolves to its first occurrence.
 * Every application runs under the [limits] of the patch: the nodes it adds count against
 * [YamlLimits.maximumNodes], its recursion is bounded by [YamlLimits.maximumDepth], and in a syntax tree it makes at
 * most [YamlLimits.maximumPatchOperations] changes.
 *
 * A merge patch is immutable and safe to share between threads.
 *
 * @property patch the patch; a node that is not a mapping replaces the target as a whole.
 * @property limits the limits every application of the patch runs under.
 * @since 1.0.0
 */
public class JsonMergePatch(public val patch: YamlNode, public val limits: YamlLimits = YamlLimits.Untrusted) {
    /**
     * Applies this patch to the graph [target], which stays unchanged.
     *
     * @param target the root of the graph to patch.
     * @return the patched graph.
     * @throws YamlDocumentLimitException if the patch adds more nodes than [YamlLimits.maximumNodes] or descends
     * deeper than [YamlLimits.maximumDepth].
     * @since 1.0.0
     */
    public fun apply(target: YamlNode): YamlNode = MergePatching(limits).merged(target, patch)

    /**
     * Applies this patch in place to the document at [document] of [tree], keeping its comments, styles and layout.
     *
     * The document is composed once before and once after the edits; when the edited tree does not give the graph
     * the patch gives, the patch fails with [JsonPatchRefusal.Unsupported] and nothing is changed.
     *
     * @param tree the syntax tree to edit, which stays unchanged.
     * @param document the index of the document in [tree], from zero.
     * @return the edited tree and its text edits, or why it failed, with the index `0`.
     * @since 1.0.0
     */
    public fun apply(tree: YamlSyntaxTree, document: Int = 0): JsonPatchEdit =
        MergePatchEditing(tree, document, limits).apply(patch)

    /**
     * Returns whether [other] is a merge patch with an equal patch.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is a [JsonMergePatch] with an equal [patch].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is JsonMergePatch && patch == other.patch

    /**
     * Returns the hash code of the patch.
     *
     * @return the hash code of [patch].
     * @since 1.0.0
     */
    override fun hashCode(): Int = patch.hashCode()

    /**
     * Returns the kind of the patch, never its content, which may be sensitive.
     *
     * @return a description such as `JsonMergePatch(Mapping)`.
     * @since 1.0.0
     */
    override fun toString(): String = "JsonMergePatch(${patch.kind})"
}
