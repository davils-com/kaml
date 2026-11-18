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

package com.davils.kaml.core.syntax.edit

/**
 * Why a [YamlEditor] refused an edit; the tree it was asked to edit stays as it is.
 *
 * @since 1.0.0
 */
public enum class YamlEditRefusal {
    /**
     * The tree has no document at the index the editor was created for.
     *
     * @since 1.0.0
     */
    MissingDocument,

    /**
     * The path addresses no value: a key or an index on the way does not exist, the path is empty where the edit
     * needs a parent, or the value is not where the edit expects it.
     *
     * @since 1.0.0
     */
    MissingPath,

    /**
     * The path ends with a key, but its parent is not a mapping.
     *
     * @since 1.0.0
     */
    NotAMapping,

    /**
     * The path ends with an index or addresses the target of an append, but the value there is not a sequence.
     *
     * @since 1.0.0
     */
    NotASequence,

    /**
     * The edit cannot be written without changing the layout around it or the meaning of the document: for example
     * an entry without a value, a comment on an entry of a flow mapping, an item that shares its line with its parent,
     * a key that is too long to be written as an implicit key, or a flow value when the emitter does not preserve
     * collection styles.
     *
     * @since 1.0.0
     */
    Unsupported,

    /**
     * The edit would remove or replace a value or a key whose anchor an alias later in the document refers to, so the
     * alias would refer to an earlier anchor of the same name or to none.
     *
     * @since 1.0.0
     */
    AnchorInUse,
}
