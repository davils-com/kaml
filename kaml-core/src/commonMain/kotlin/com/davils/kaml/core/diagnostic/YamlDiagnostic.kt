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

@file:OptIn(DavilsInternal::class)

package com.davils.kaml.core.diagnostic

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.validation.violation.Severity

private const val HASH_FACTOR: Int = 31

/**
 * A problem kaml found in a YAML source, such as a syntax error, a duplicate key or an exceeded limit.
 *
 * The [message] is meant for people and may change between versions; [code] is stable and meant for programs. The
 * constructor escapes the message through [SafeText] and shortens it to [Limits.MAX_MESSAGE_LENGTH] UTF-16 code units,
 * so whatever part of a document a message quotes, it can neither forge a log line nor control a terminal, and its
 * length stays bounded.
 *
 * A diagnostic is an immutable value and safe to share between threads; two diagnostics are equal when their codes,
 * ranges, messages and severities are equal.
 *
 * @property code the kind of the problem.
 * @property range the place of the problem in the source, in UTF-16 offsets of the decoded text.
 * @property severity whether the problem is an error or a warning.
 * @since 1.0.0
 */
public class YamlDiagnostic private constructor(
    public val code: YamlDiagnosticCode,
    public val range: TextRange,
    public val severity: Severity,
    shownMessage: String,
) {
    /**
     * The description of the problem, escaped through [SafeText] and at most [Limits.MAX_MESSAGE_LENGTH] code units
     * long plus the mark of the characters left out.
     *
     * @since 1.0.0
     */
    public val message: String = shownMessage

    /**
     * Creates a diagnostic of [code] at [range] with [message], escaped and bounded as the class describes.
     *
     * @param code the kind of the problem.
     * @param range the place of the problem in the source.
     * @param message the description of the problem, which may quote the source and hold any character.
     * @param severity whether the problem is an error or a warning; the severity of [code] by default.
     * @since 1.0.0
     */
    public constructor(
        code: YamlDiagnosticCode,
        range: TextRange,
        message: String,
        severity: Severity = code.severity,
    ) : this(
        code = code,
        range = range,
        severity = severity,
        shownMessage = SafeText.text(message, Limits.MAX_MESSAGE_LENGTH),
    )

    internal fun movedTo(range: TextRange): YamlDiagnostic =
        YamlDiagnostic(code = code, range = range, severity = severity, shownMessage = message)

    /**
     * Returns whether [other] is a diagnostic with the same code, range, message and severity.
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is an equal diagnostic, `false` otherwise.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        val isSame = other is YamlDiagnostic && code == other.code && range == other.range
        return isSame && other.message == message && other.severity == severity
    }

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code of the code, the range, the message and the severity.
     * @since 1.0.0
     */
    override fun hashCode(): Int {
        val located = HASH_FACTOR * code.hashCode() + range.hashCode()
        return HASH_FACTOR * (HASH_FACTOR * located + message.hashCode()) + severity.hashCode()
    }

    /**
     * Returns the diagnostic with its code, severity, range and escaped message, so the result is bounded and safe to
     * log.
     *
     * @return a text such as `YamlDiagnostic(code=kaml.mapping.duplicate-key, severity=Error, range=0..1, message=…)`.
     * @since 1.0.0
     */
    override fun toString(): String = "YamlDiagnostic(code=$code, severity=$severity, range=$range, message=$message)"
}
