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

import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder behind [yamlSequence] with a block, which adds one item per call of `add`, such as
 * `yamlSequence { add("a"); add(1) }`.
 *
 * A builder is not thread-safe and belongs to the block it is given to.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class YamlSequenceBuilder internal constructor() {
    private val collected = ArrayList<YamlNode>()

    internal val items: List<YamlNode> get() = buildList { addAll(collected) }

    /**
     * Adds [value] as the next item.
     *
     * @param value the item.
     * @since 1.0.0
     */
    public fun add(value: YamlNode) {
        collected.add(value)
    }

    /**
     * Adds a string scalar of [value] as the next item.
     *
     * @param value the value of the item.
     * @since 1.0.0
     */
    public fun add(value: String) {
        add(yamlScalar(value))
    }

    /**
     * Adds an integer scalar of [value] as the next item.
     *
     * @param value the value of the item.
     * @since 1.0.0
     */
    public fun add(value: Long) {
        add(yamlScalar(value))
    }

    /**
     * Adds an integer scalar of [value] as the next item.
     *
     * @param value the value of the item.
     * @since 1.0.0
     */
    public fun add(value: Int) {
        add(yamlScalar(value))
    }

    /**
     * Adds a float scalar of [value] as the next item.
     *
     * @param value the value of the item.
     * @since 1.0.0
     */
    public fun add(value: Double) {
        add(yamlScalar(value))
    }

    /**
     * Adds a boolean scalar of [value] as the next item.
     *
     * @param value the value of the item.
     * @since 1.0.0
     */
    public fun add(value: Boolean) {
        add(yamlScalar(value))
    }
}
