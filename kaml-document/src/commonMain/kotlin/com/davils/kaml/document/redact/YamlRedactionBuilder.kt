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

package com.davils.kaml.document.redact

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures a [YamlRedaction]: what is secret in a document and what stands in its place.
 *
 * A builder is not safe to share between threads; the redaction it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class YamlRedactionBuilder internal constructor() : ValidatingBuilder<YamlRedaction>() {
    override val subject: String get() = "yaml redaction"

    /**
     * The text that replaces a redacted value or the matched part of a text; `***` by default. It is written
     * literally, so `$0` or a backslash stays as it is, and must not contain a line break.
     *
     * @since 1.0.0
     */
    public var replacement: String = DEFAULT_REPLACEMENT

    /**
     * The limits the redaction runs under: those of its JSONPath queries, [YamlLimits.maximumNodes] and
     * [YamlLimits.maximumDepth] for its walks, and [YamlLimits.maximumPatternInputLength] for the text a value or
     * comment pattern may read; [YamlLimits.Untrusted] by default.
     *
     * @since 1.0.0
     */
    public var limits: YamlLimits = YamlLimits.Untrusted

    /**
     * Whether the comments on the lines of a redacted entry, from its key to the end of its value, are redacted as
     * well, since they often describe the secret; `true` by default.
     *
     * @since 1.0.0
     */
    public var isTrailingCommentRedacted: Boolean = true

    internal val paths: MutableList<String> = mutableListOf()

    internal val keys: MutableList<(String) -> Boolean> = mutableListOf()

    internal val values: MutableList<Regex> = mutableListOf()

    internal val comments: MutableList<Regex> = mutableListOf()

    /**
     * Marks every node the JSONPath query [expression] selects as secret.
     *
     * @param expression the query of RFC 9535, parsed with [limits] when the redaction is built; an invalid query
     * fails the build.
     * @since 1.0.0
     */
    public fun path(expression: String) {
        checkOpen()
        paths += expression
    }

    /**
     * Marks the value of every member whose key [predicate] accepts as secret, wherever the member occurs.
     *
     * @param predicate receives the text of a key, which may come from an attacker. When it throws an
     * [Exception], the redaction fails with an [IllegalStateException] that carries neither the key nor the
     * exception.
     * @since 1.0.0
     */
    public fun key(predicate: (String) -> Boolean) {
        checkOpen()
        keys += predicate
    }

    /**
     * Replaces every match of [pattern] in a scalar value with the [replacement].
     *
     * The pattern runs on the regular expression engine of the platform, which may take exponential time, so it
     * only ever sees a value of at most [YamlLimits.maximumPatternInputLength] characters; a longer value fails the
     * redaction. It sees values, not keys.
     *
     * @param pattern the pattern, supplied by the caller and never by a document.
     * @since 1.0.0
     */
    public fun value(pattern: Regex) {
        checkOpen()
        values += pattern
    }

    /**
     * Replaces every match of [pattern] in the text of a comment after its `#` with the [replacement].
     *
     * As for [value], the pattern only ever sees at most [YamlLimits.maximumPatternInputLength] characters.
     *
     * @param pattern the pattern, supplied by the caller and never by a document.
     * @since 1.0.0
     */
    public fun comment(pattern: Regex) {
        checkOpen()
        comments += pattern
    }

    override fun assemble(): YamlRedaction = YamlRedaction(this)

    private companion object {
        const val DEFAULT_REPLACEMENT: String = "***"
    }
}
