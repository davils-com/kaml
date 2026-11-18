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

import com.davils.kaml.core.diagnostic.YamlDiagnostic

private const val HASH_FACTOR: Int = 31

/**
 * The outcome of decoding YAML with [YamlFormat.decodeOrFailure] or [YamlFormat.decodeAll]: a value, or the
 * problems that prevented it.
 *
 * Every diagnostic carries the position of the node it refers to when the node was read from text; a node built in
 * code has none, and its diagnostics point at the start. Messages never contain raw input: names and values from the
 * document are escaped and bounded, and the message of an exception thrown by a serializer is replaced by a redacted
 * placeholder.
 *
 * @param T the type of the decoded value.
 * @since 1.0.0
 */
public sealed interface YamlDecoding<out T> {
    /**
     * The problems found while reading and decoding, in the order they were found. A [Decoded] value carries only
     * warnings; a [Failed] one at least one error.
     *
     * @since 1.0.0
     */
    public val diagnostics: List<YamlDiagnostic>

    /**
     * A decoded [value], together with the warnings found on the way.
     *
     * @param T the type of the decoded value.
     * @property value the decoded value.
     * @param diagnostics the warnings found while reading and decoding, copied into an immutable list.
     * @since 1.0.0
     */
    public class Decoded<out T> internal constructor(
        public val value: T,
        diagnostics: List<YamlDiagnostic>,
    ) : YamlDecoding<T> {
        override val diagnostics: List<YamlDiagnostic> = buildList { addAll(diagnostics) }

        override fun equals(other: Any?): Boolean =
            other is Decoded<*> && value == other.value && diagnostics == other.diagnostics

        override fun hashCode(): Int = HASH_FACTOR * value.hashCode() + diagnostics.hashCode()

        override fun toString(): String = "Decoded(diagnostics=${diagnostics.size})"
    }

    /**
     * A decoding that failed, with the problems that explain why.
     *
     * @since 1.0.0
     */
    public class Failed internal constructor(diagnostics: List<YamlDiagnostic>) : YamlDecoding<Nothing> {
        override val diagnostics: List<YamlDiagnostic> = buildList { addAll(diagnostics) }

        override fun equals(other: Any?): Boolean = other is Failed && diagnostics == other.diagnostics

        override fun hashCode(): Int = diagnostics.hashCode()

        override fun toString(): String = "Failed(diagnostics=${diagnostics.size})"
    }
}
