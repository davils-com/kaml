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

/**
 * How a later layer of a merge marks a value as deleted, so that it removes what it replaces.
 *
 * @since 1.0.0
 */
public enum class YamlDeletion {
    /**
     * Nothing is a deletion; every value is merged as it is.
     *
     * @since 1.0.0
     */
    None,

    /**
     * A value with the local tag `!delete` is a deletion, such as `port: !delete`.
     *
     * @since 1.0.0
     */
    Tag,

    /**
     * A null value is a deletion, as in a JSON Merge Patch.
     *
     * @since 1.0.0
     */
    Null,
}
