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

package com.davils.kaml.serialization

import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * Derives the YAML name of a property from its serial name.
 *
 * A [YamlFormat] asks the strategy for the name of every property of a class, both when it encodes and when it
 * decodes. Two properties, or a property and an alias of [com.davils.kaml.serialization.annotation.YamlNames], that end
 * up with the same name are a mistake in the class and fail with an [IllegalStateException] as soon as the class is
 * encoded or decoded. A strategy must be pure and safe to call from several threads at once, because a format is.
 *
 * @since 1.0.0
 */
public fun interface YamlNamingStrategy {
    /**
     * Returns the YAML name of the property at [index] of [descriptor].
     *
     * @param descriptor the descriptor of the class that declares the property.
     * @param index the index of the property in [descriptor].
     * @param serialName the serial name of the property, after `@SerialName`.
     * @return the name the property has in YAML.
     * @since 1.0.0
     */
    public fun nameOf(descriptor: SerialDescriptor, index: Int, serialName: String): String

    /**
     * The predefined strategies.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Keeps the serial name as it is.
         *
         * @since 1.0.0
         */
        public val Identity: YamlNamingStrategy = YamlNamingStrategy { _, _, serialName -> serialName }

        /**
         * Writes the serial name in lower case with an underscore before every word, so `maxConnections` becomes
         * `max_connections`. A run of capitals counts as one word, so `dnsTTL` becomes `dns_ttl`.
         *
         * @since 1.0.0
         */
        public val SnakeCase: YamlNamingStrategy = YamlNamingStrategy { _, _, serialName -> words(serialName, '_') }

        /**
         * Writes the serial name in lower case with a hyphen before every word, so `maxConnections` becomes
         * `max-connections`. A run of capitals counts as one word, so `dnsTTL` becomes `dns-ttl`.
         *
         * @since 1.0.0
         */
        public val KebabCase: YamlNamingStrategy = YamlNamingStrategy { _, _, serialName -> words(serialName, '-') }

        private fun words(name: String, separator: Char): String {
            val result = StringBuilder(name.length + name.length / 2)
            name.forEachIndexed { index, character ->
                val isBoundary = character.isUpperCase() && index > 0 && !name[index - 1].isUpperCase()
                if (isBoundary) result.append(separator)
                result.append(character.lowercaseChar())
            }
            return result.toString()
        }
    }
}
