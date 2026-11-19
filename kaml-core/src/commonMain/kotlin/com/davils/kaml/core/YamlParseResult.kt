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

package com.davils.kaml.core

import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.node.YamlStream

private const val HASH_FACTOR: Int = 31

/**
 * The result of parsing a YAML source into its representation graph with [Yaml.parse].
 *
 * A source without errors is [Parsed]; a source with at least one error is [Failed] and keeps the part of the graph
 * that could be built. Warnings, such as a newer minor version of YAML, leave a source [Parsed]. Either result holds
 * at most `YamlLimits.maximumDiagnostics` diagnostics, plus one
 * [YamlDiagnosticCode.TooManyDiagnostics] diagnostic when more were found.
 *
 * Results are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public sealed interface YamlParseResult {
    /**
     * The problems found in the source, ordered by their place in it; warnings only for a [Parsed] result.
     *
     * @since 1.0.0
     */
    public val diagnostics: List<YamlDiagnostic>

    /**
     * A source that was parsed without errors.
     *
     * @property stream the documents of the source.
     * @param diagnostics the warnings found in the source; the result keeps a copy.
     * @since 1.0.0
     */
    public class Parsed(public val stream: YamlStream, diagnostics: List<YamlDiagnostic>) : YamlParseResult {
        /**
         * The warnings found in the source, ordered by their place in it.
         *
         * @since 1.0.0
         */
        override val diagnostics: List<YamlDiagnostic> = buildList { addAll(diagnostics) }

        /**
         * Returns whether [other] is a parsed result with an equal stream and equal diagnostics.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal result, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean =
            other is Parsed && stream == other.stream && diagnostics == other.diagnostics

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code of the stream and the diagnostics.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * stream.hashCode() + diagnostics.hashCode()

        /**
         * Returns the number of documents and diagnostics, which is bounded and shows nothing of the source.
         *
         * @return a text such as `Parsed(documents=1, diagnostics=0)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Parsed(documents=${stream.documents.size}, diagnostics=${diagnostics.size})"
    }

    /**
     * A source with at least one error.
     *
     * @param diagnostics the problems found in the source; the result keeps a copy.
     * @property partial the documents, or the parts of them, that could be built before or despite the errors. It
     * holds no node beyond the limit that stopped the work, and a mapping in it keeps only the first of several equal
     * keys, and a character the source cannot hold, such as an escape of a lone surrogate, is replaced by U+FFFD. It
     * is meant for tools such as editors; a caller that needs a valid document uses [Parsed] only.
     * @since 1.0.0
     */
    public class Failed(diagnostics: List<YamlDiagnostic>, public val partial: YamlStream) : YamlParseResult {
        /**
         * The problems found in the source, ordered by their place in it, with at least one error.
         *
         * @since 1.0.0
         */
        override val diagnostics: List<YamlDiagnostic> = buildList { addAll(diagnostics) }

        /**
         * Returns whether [other] is a failed result with equal diagnostics and an equal partial stream.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal result, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean =
            other is Failed && diagnostics == other.diagnostics && partial == other.partial

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code of the diagnostics and the partial stream.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * diagnostics.hashCode() + partial.hashCode()

        /**
         * Returns the number of diagnostics and of partial documents, which is bounded and shows nothing of the
         * source.
         *
         * @return a text such as `Failed(diagnostics=2, documents=1)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Failed(diagnostics=${diagnostics.size}, documents=${partial.documents.size})"
    }
}
