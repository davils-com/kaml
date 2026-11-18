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
 * The builder behind [yamlMapping] with a block, which adds one entry per call of `to`, such as
 * `yamlMapping { "port" to 8080; yamlScalar(1) to "one" }`.
 *
 * A key is a string scalar unless it is given as a node. Every key must differ from the keys added before it, compared
 * as nodes are compared, so the mapping it builds has no repeated key. A builder is not thread-safe and belongs to the
 * block it is given to.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class YamlMappingBuilder internal constructor() {
    private val collected = ArrayList<YamlEntry>()
    private val keys = NodeKeySet()

    internal val entries: List<YamlEntry> get() = buildList { addAll(collected) }

    /**
     * Adds an entry with a string scalar of this text as its key and [value] as its value.
     *
     * @param value the value of the entry.
     * @throws IllegalArgumentException if the mapping already has this key.
     * @since 1.0.0
     */
    public infix fun String.to(value: YamlNode) {
        add(key = yamlScalar(this), value = value)
    }

    /**
     * Adds an entry with a string scalar of this text as its key and a string scalar of [value] as its value.
     *
     * @param value the value of the entry.
     * @throws IllegalArgumentException if the mapping already has this key.
     * @since 1.0.0
     */
    public infix fun String.to(value: String) {
        to(yamlScalar(value))
    }

    /**
     * Adds an entry with a string scalar of this text as its key and an integer scalar of [value] as its value.
     *
     * @param value the value of the entry.
     * @throws IllegalArgumentException if the mapping already has this key.
     * @since 1.0.0
     */
    public infix fun String.to(value: Long) {
        to(yamlScalar(value))
    }

    /**
     * Adds an entry with a string scalar of this text as its key and an integer scalar of [value] as its value.
     *
     * @param value the value of the entry.
     * @throws IllegalArgumentException if the mapping already has this key.
     * @since 1.0.0
     */
    public infix fun String.to(value: Int) {
        to(yamlScalar(value))
    }

    /**
     * Adds an entry with a string scalar of this text as its key and a float scalar of [value] as its value.
     *
     * @param value the value of the entry.
     * @throws IllegalArgumentException if the mapping already has this key.
     * @since 1.0.0
     */
    public infix fun String.to(value: Double) {
        to(yamlScalar(value))
    }

    /**
     * Adds an entry with a string scalar of this text as its key and a boolean scalar of [value] as its value.
     *
     * @param value the value of the entry.
     * @throws IllegalArgumentException if the mapping already has this key.
     * @since 1.0.0
     */
    public infix fun String.to(value: Boolean) {
        to(yamlScalar(value))
    }

    /**
     * Adds an entry with this node as its key and [value] as its value.
     *
     * @param value the value of the entry.
     * @throws IllegalArgumentException if the mapping already has a key equal to this node.
     * @since 1.0.0
     */
    public infix fun YamlNode.to(value: YamlNode) {
        add(key = this, value = value)
    }

    /**
     * Adds an entry with this node as its key and a string scalar of [value] as its value.
     *
     * @param value the value of the entry.
     * @throws IllegalArgumentException if the mapping already has a key equal to this node.
     * @since 1.0.0
     */
    public infix fun YamlNode.to(value: String) {
        add(key = this, value = yamlScalar(value))
    }

    /**
     * Adds an entry with this node as its key and an integer scalar of [value] as its value.
     *
     * @param value the value of the entry.
     * @throws IllegalArgumentException if the mapping already has a key equal to this node.
     * @since 1.0.0
     */
    public infix fun YamlNode.to(value: Long) {
        add(key = this, value = yamlScalar(value))
    }

    /**
     * Adds an entry with this node as its key and an integer scalar of [value] as its value.
     *
     * @param value the value of the entry.
     * @throws IllegalArgumentException if the mapping already has a key equal to this node.
     * @since 1.0.0
     */
    public infix fun YamlNode.to(value: Int) {
        add(key = this, value = yamlScalar(value))
    }

    /**
     * Adds an entry with this node as its key and a float scalar of [value] as its value.
     *
     * @param value the value of the entry.
     * @throws IllegalArgumentException if the mapping already has a key equal to this node.
     * @since 1.0.0
     */
    public infix fun YamlNode.to(value: Double) {
        add(key = this, value = yamlScalar(value))
    }

    /**
     * Adds an entry with this node as its key and a boolean scalar of [value] as its value.
     *
     * @param value the value of the entry.
     * @throws IllegalArgumentException if the mapping already has a key equal to this node.
     * @since 1.0.0
     */
    public infix fun YamlNode.to(value: Boolean) {
        add(key = this, value = yamlScalar(value))
    }

    private fun add(key: YamlNode, value: YamlNode) {
        require(keys.add(key)) { "A mapping holds every key once, but entry ${collected.size} repeats an earlier key" }
        collected.add(YamlEntry(key = key, value = value))
    }
}
