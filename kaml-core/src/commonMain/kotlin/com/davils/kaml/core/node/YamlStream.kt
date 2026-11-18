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

/**
 * The documents of a YAML source, in order.
 *
 * A stream is immutable and safe to share between threads; two streams are equal when they hold equal documents in
 * the same order.
 *
 * @param documents the documents in order; the stream keeps a copy.
 * @since 1.0.0
 */
public class YamlStream(documents: List<YamlDocument>) {
    /**
     * The documents of the stream, in order.
     *
     * @since 1.0.0
     */
    public val documents: List<YamlDocument> = buildList { addAll(documents) }

    /**
     * The only document of the stream, or `null` if it has no or several documents.
     *
     * @since 1.0.0
     */
    public val single: YamlDocument? get() = documents.singleOrNull()

    /**
     * Returns whether [other] is a stream with equal documents in the same order.
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is an equal stream, `false` otherwise.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is YamlStream && documents == other.documents

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code of the documents.
     * @since 1.0.0
     */
    override fun hashCode(): Int = documents.hashCode()

    /**
     * Returns the number of documents, which is bounded and shows no content.
     *
     * @return a text such as `YamlStream(documents=2)`.
     * @since 1.0.0
     */
    override fun toString(): String = "YamlStream(documents=${documents.size})"
}
