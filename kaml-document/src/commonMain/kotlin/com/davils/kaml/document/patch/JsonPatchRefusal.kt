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

/**
 * Why a JSON Patch or a JSON Merge Patch could not be applied.
 *
 * @since 1.0.0
 */
public enum class JsonPatchRefusal {
    /**
     * The node an operation reads, replaces or removes does not exist.
     *
     * @since 1.0.0
     */
    MissingTarget,

    /**
     * The parent of the node an operation adds does not exist.
     *
     * @since 1.0.0
     */
    MissingParent,

    /**
     * The index of an item is neither an index of the sequence, nor its size when adding, nor `-`.
     *
     * @since 1.0.0
     */
    InvalidIndex,

    /**
     * An operation adds to or removes from a scalar.
     *
     * @since 1.0.0
     */
    NotAContainer,

    /**
     * A `remove` names the root, which cannot be removed.
     *
     * @since 1.0.0
     */
    RootRemoval,

    /**
     * A `move` would move a node into one of its own descendants.
     *
     * @since 1.0.0
     */
    MoveIntoItself,

    /**
     * A `test` found a value that differs from the one it expects.
     *
     * @since 1.0.0
     */
    TestFailed,

    /**
     * The syntax tree has no document at the requested index.
     *
     * @since 1.0.0
     */
    MissingDocument,

    /**
     * The syntax tree does not compose without errors, before or after the patch.
     *
     * @since 1.0.0
     */
    InvalidDocument,

    /**
     * The syntax tree cannot be edited in place to give the result the patch gives on the composed graph, for example
     * because the change goes through an alias, a merge key or a duplicate key, or a merge patch names a member by a
     * key that is not a scalar. Nothing is changed.
     *
     * @since 1.0.0
     */
    Unsupported,

    /**
     * The patch needs more than one of the limits it runs under allows: more nodes than
     * [com.davils.kaml.core.limit.YamlLimits.maximumNodes], a deeper result than
     * [com.davils.kaml.core.limit.YamlLimits.maximumDepth] or more changes than
     * [com.davils.kaml.core.limit.YamlLimits.maximumPatchOperations]. Nothing is changed.
     *
     * @since 1.0.0
     */
    LimitExceeded,
}
