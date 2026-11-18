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

package com.davils.kaml.core.event

import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kore.core.validation.violation.Severity

private const val HASH_FACTOR: Int = 31

/**
 * The events of a source and the problems found while reading them, the result of [com.davils.kaml.core.Yaml.events].
 *
 * A source with errors still has the events that could be read; [isValid] tells whether there were errors. A result
 * is immutable and safe to share between threads.
 *
 * @param events the events of the source, in order; the result keeps a copy.
 * @param diagnostics the problems found in the source; the result keeps a copy.
 * @since 1.0.0
 */
public class YamlEventResult(events: List<YamlEvent>, diagnostics: List<YamlDiagnostic>) {
    /**
     * The events of the source, in order.
     *
     * @since 1.0.0
     */
    public val events: List<YamlEvent> = buildList { addAll(events) }

    /**
     * The problems found in the source, ordered by their place in it.
     *
     * @since 1.0.0
     */
    public val diagnostics: List<YamlDiagnostic> = buildList { addAll(diagnostics) }

    /**
     * Whether the source has no error; warnings alone keep it valid.
     *
     * @since 1.0.0
     */
    public val isValid: Boolean get() = diagnostics.none { it.severity == Severity.Error }

    /**
     * Returns whether [other] is a result with equal events and equal diagnostics.
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is an equal result, `false` otherwise.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is YamlEventResult && events == other.events && diagnostics == other.diagnostics

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code of the events and the diagnostics.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * events.hashCode() + diagnostics.hashCode()

    /**
     * Returns the number of events and diagnostics, which is bounded and shows nothing of the source.
     *
     * @return a text such as `YamlEventResult(events=7, diagnostics=0)`.
     * @since 1.0.0
     */
    override fun toString(): String = "YamlEventResult(events=${events.size}, diagnostics=${diagnostics.size})"
}
