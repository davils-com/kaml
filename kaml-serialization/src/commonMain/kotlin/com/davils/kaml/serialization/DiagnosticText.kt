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
import com.davils.kore.core.Limits
import com.davils.kore.core.text.LineMap

private const val HEADING: String = "The YAML could not be decoded:"

internal object DiagnosticText {
    fun render(text: String, diagnostics: List<YamlDiagnostic>): String {
        val indexed = text.takeIf { it.length <= Limits.MAX_LINE_MAP_LENGTH }.orEmpty()
        val lines = LineMap.of(indexed)
        val message = StringBuilder(HEADING)
        var shown = 0
        for (diagnostic in diagnostics) {
            val line = "\n" + lineOf(lines, indexed, diagnostic)
            val isFull = shown > 0 && message.length + line.length > Limits.MAX_MESSAGE_LENGTH
            if (isFull) break
            message.append(line)
            shown++
        }
        val hidden = diagnostics.size - shown
        if (hidden > 0) message.append("\nand $hidden more problems")
        return message.toString()
    }

    private fun lineOf(lines: LineMap, text: String, diagnostic: YamlDiagnostic): String {
        val offset = diagnostic.range.start.coerceAtMost(text.length)
        val position = lines.positionOf(offset)
        return "$position ${diagnostic.message} [${diagnostic.code}]"
    }
}
