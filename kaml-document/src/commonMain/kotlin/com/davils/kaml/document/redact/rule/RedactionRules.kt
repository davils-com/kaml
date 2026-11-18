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

package com.davils.kaml.document.redact.rule

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.document.path.JsonPath

internal class RedactionRules(
    val replacement: String,
    val paths: List<JsonPath>,
    private val keys: List<(String) -> Boolean>,
    private val values: List<Regex>,
    private val comments: List<Regex>,
    val limits: YamlLimits,
    val isTrailingCommentRedacted: Boolean,
) {
    val hasValuePatterns: Boolean get() = values.isNotEmpty()

    val hasCommentPatterns: Boolean get() = comments.isNotEmpty()

    val hasKeys: Boolean get() = keys.isNotEmpty()

    fun isSecretKey(key: String): Boolean = keys.any { predicate -> accepted(predicate, key) }

    fun value(text: String): String? = replaced(values, text)

    fun comment(text: String): String? = replaced(comments, text)

    private fun replaced(patterns: List<Regex>, text: String): String? {
        if (patterns.isEmpty()) return text
        val isReadable = text.length <= limits.maximumPatternInputLength
        return text.takeIf { isReadable }?.let { readable ->
            patterns.fold(readable) { current, pattern -> pattern.replace(current) { replacement } }
        }
    }

    private fun accepted(predicate: (String) -> Boolean, key: String): Boolean {
        val outcome = runCatching { predicate(key) }
        val failure = outcome.exceptionOrNull()
        if (failure is Error) throw failure
        return checkNotNull(outcome.getOrNull()) { "A key predicate of the redaction failed" }
    }
}
