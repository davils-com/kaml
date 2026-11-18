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

package com.davils.kaml.core.model

private const val HASH_FACTOR: Int = 31

/**
 * The version of YAML a document declares with its `%YAML` directive, such as `1.2`.
 *
 * Versions are ordered by [major] first and by [minor] second. A version is an immutable value and safe to share
 * between threads; two versions are equal when both numbers are equal.
 *
 * @property major the major version, zero or more.
 * @property minor the minor version, zero or more.
 * @throws IllegalArgumentException if [major] or [minor] is negative.
 * @since 1.0.0
 */
public class YamlVersion(public val major: Int, public val minor: Int) : Comparable<YamlVersion> {
    init {
        require(major >= 0 && minor >= 0) { "A YAML version has no negative numbers, but was $major.$minor" }
    }

    /**
     * Compares this version with [other] by [major] first and by [minor] second.
     *
     * @param other the version to compare with.
     * @return a negative number, zero or a positive number as this version is older than, equal to or newer than
     * [other].
     * @since 1.0.0
     */
    override fun compareTo(other: YamlVersion): Int = versionOrder.compare(this, other)

    /**
     * Returns whether [other] is a version with the same [major] and [minor] numbers.
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is an equal version, `false` otherwise.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is YamlVersion && major == other.major && minor == other.minor

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code of both numbers.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * major + minor

    /**
     * Returns the version as a `%YAML` directive writes it, such as `1.2`.
     *
     * @return the major and the minor number, separated by a dot.
     * @since 1.0.0
     */
    override fun toString(): String = "$major.$minor"

    /**
     * The versions kaml reads.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * YAML 1.1, whose documents are read with the schema of YAML 1.1 when the configuration selects the schema by
         * version.
         *
         * @since 1.0.0
         */
        public val V1_1: YamlVersion = YamlVersion(major = 1, minor = 1)

        /**
         * YAML 1.2, the version kaml implements.
         *
         * @since 1.0.0
         */
        public val V1_2: YamlVersion = YamlVersion(major = 1, minor = 2)

        private val versionOrder: Comparator<YamlVersion> = compareBy({ it.major }, { it.minor })
    }
}
