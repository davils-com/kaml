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

package com.davils.kaml.core.compose

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.model.VisibleCodePoints
import com.davils.kaml.core.model.YamlTagDirective
import com.davils.kaml.core.model.YamlVersion
import com.davils.kaml.core.parser.isWordCharacter
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.TextRange

internal class DocumentDirectives(private val reports: Reports) {
    private val handles = LinkedHashMap<String, String>()

    var version: YamlVersion? = null
        private set

    val tagDirectives: List<YamlTagDirective>
        get() = handles.map { (handle, prefix) -> YamlTagDirective(handle = handle, prefix = prefix) }

    fun prefixOf(handle: String): String? = handles[handle] ?: defaults[handle]

    fun read(name: String, parameters: List<String>, range: TextRange) {
        val isHandled = yaml(name, parameters, range) || tag(name, parameters, range)
        if (!isHandled) {
            reports.add(
                YamlDiagnosticCode.ReservedDirective,
                range,
                "The directive ${SafeText.quoted(name)} is reserved and ignored",
            )
        }
    }

    private fun yaml(name: String, parameters: List<String>, range: TextRange): Boolean {
        if (name != YAML_DIRECTIVE) return false
        val parsed = parameters.singleOrNull()?.let(::versionOf)
        if (parsed == null) {
            reports.add(
                YamlDiagnosticCode.InvalidDirective,
                range,
                "%YAML needs exactly one version such as 1.2",
            )
        }
        if (parsed != null) acceptVersion(parsed, range)
        return true
    }

    private fun acceptVersion(parsed: YamlVersion, range: TextRange) {
        if (version != null) {
            reports.add(
                YamlDiagnosticCode.DuplicateDirective,
                range,
                "A document has at most one %YAML directive",
            )
        }
        checkSupported(parsed, range)
        version = parsed
    }

    private fun checkSupported(parsed: YamlVersion, range: TextRange) {
        if (parsed.major != 1) {
            reports.add(YamlDiagnosticCode.UnsupportedVersion, range, "YAML $parsed is not supported")
        }
        if (parsed.major == 1 && parsed > YamlVersion.V1_2) {
            reports.add(YamlDiagnosticCode.NewerMinorVersion, range, "YAML $parsed is read as YAML 1.2")
        }
    }

    private fun tag(name: String, parameters: List<String>, range: TextRange): Boolean {
        if (name != TAG_DIRECTIVE) return false
        val isValid = parameters.size == 2 && isHandle(parameters[0]) && VisibleCodePoints.isVisibleText(parameters[1])
        if (!isValid) {
            reports.add(
                YamlDiagnosticCode.InvalidDirective,
                range,
                "%TAG needs a handle such as !e! and a prefix",
            )
        }
        if (isValid) acceptHandle(handle = parameters[0], prefix = parameters[1], range = range)
        return true
    }

    private fun acceptHandle(handle: String, prefix: String, range: TextRange) {
        if (handle in handles) {
            reports.add(
                YamlDiagnosticCode.DuplicateDirective,
                range,
                "The handle ${SafeText.quoted(handle)} is declared twice",
            )
        }
        handles[handle] = prefix
    }

    private fun isHandle(text: String): Boolean {
        val isNamed = text.length > 2 &&
            text.first() == '!' &&
            text.last() == '!' &&
            text.substring(startIndex = 1, endIndex = text.length - 1).all(Char::isWordCharacter)
        return text == PRIMARY || text == SECONDARY || isNamed
    }

    private fun versionOf(text: String): YamlVersion? {
        val parts = text.split('.')
        val numbers = parts.mapNotNull { part -> part.takeIf(::isNumber)?.toIntOrNull() }
        if (parts.size != 2 || numbers.size != 2) return null
        return YamlVersion(major = numbers[0], minor = numbers[1])
    }

    private fun isNumber(text: String): Boolean = text.isNotEmpty() && text.all { it in '0'..'9' }

    private companion object {
        const val YAML_DIRECTIVE: String = "%YAML"
        const val TAG_DIRECTIVE: String = "%TAG"
        const val PRIMARY: String = "!"
        const val SECONDARY: String = "!!"

        val defaults: Map<String, String> = mapOf(PRIMARY to PRIMARY, SECONDARY to "tag:yaml.org,2002:")
    }
}
