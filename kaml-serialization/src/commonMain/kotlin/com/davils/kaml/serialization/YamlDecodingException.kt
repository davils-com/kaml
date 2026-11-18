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
import kotlinx.serialization.SerializationException

/**
 * Thrown by [YamlFormat.decodeFromString] and [YamlFormat.decodeFromNode] when YAML cannot be decoded.
 *
 * The message lists the problems with their line and column, escaped and bounded so that it is safe to log; it never
 * contains raw input and has no cause, so no message of a serializer leaks through it. [diagnostics] holds every
 * problem.
 *
 * @since 1.0.0
 */
public class YamlDecodingException internal constructor(
    diagnostics: List<YamlDiagnostic>,
    message: String,
) : SerializationException(message) {
    /**
     * Every problem found, in the order found, at least one of them an error.
     *
     * @since 1.0.0
     */
    public val diagnostics: List<YamlDiagnostic> = buildList { addAll(diagnostics) }
}
