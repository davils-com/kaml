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

private const val HASH_FACTOR: Int = 31

/**
 * An entry of a [YamlNode.Mapping]: a key and its value, each a node of any kind.
 *
 * An entry is an immutable value and safe to share between threads; two entries are equal when their keys are equal
 * and their values are equal.
 *
 * @param key the key of the entry.
 * @param value the value of the entry.
 * @since 1.0.0
 */
public class YamlEntry(key: YamlNode, value: YamlNode) {
    /**
     * The key of the entry.
     *
     * @since 1.0.0
     */
    public val key: YamlNode = key

    /**
     * The value of the entry.
     *
     * @since 1.0.0
     */
    public val value: YamlNode = value

    /**
     * Returns whether [other] is an entry with an equal key and an equal value.
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is an equal entry, `false` otherwise.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is YamlEntry && key == other.key && value == other.value

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code of the key and the value.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * key.hashCode() + value.hashCode()

    /**
     * Returns the key and the value in their bounded, escaped forms.
     *
     * @return a text such as `YamlEntry(key=Scalar(<tag:yaml.org,2002:str>, "a"), value=…)`.
     * @since 1.0.0
     */
    override fun toString(): String = "YamlEntry(key=$key, value=$value)"
}
