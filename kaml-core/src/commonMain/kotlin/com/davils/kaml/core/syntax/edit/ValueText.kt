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

package com.davils.kaml.core.syntax.edit

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.emit.emitValue
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlVersion
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence

internal class ValueText(
    private val yaml: Yaml,
    private val lineBreak: String,
    private val version: YamlVersion?,
    private val takenAnchors: Set<String>,
) {
    private val options = yaml.configuration.emitter
    private val indent = options.indent

    fun afterIndicator(value: YamlNode, column: Int): Rendering {
        val lines = lines(value)
        val isSameLine = isBlockScalar(value, lines) || isInline(value, lines)
        if (isSameLine) return Rendering(head = " " + lines.first(), tail = shifted(lines.drop(1), column))
        return Rendering(head = "", tail = shifted(lines, column + indent))
    }

    fun onNewLine(value: YamlNode, column: Int): Rendering {
        val lines = lines(value)
        val restColumn = column.takeIf { isBlockScalar(value, lines) } ?: column + indent
        val head = lineBreak + " ".repeat(column + indent) + lines.first()
        return Rendering(head = head, tail = shifted(lines.drop(1), restColumn))
    }

    fun item(value: YamlNode, column: Int): Rendering {
        val lines = lines(value)
        val restColumn = column.takeIf { isBlockScalar(value, lines) } ?: column + ITEM_OFFSET
        return Rendering(head = " " + lines.first(), tail = shifted(lines.drop(1), restColumn))
    }

    fun flow(value: YamlNode): String? {
        val wrapper = yamlSequence(items = listOf(value), style = YamlCollectionStyle.Flow)
        val line = lines(wrapper).singleOrNull() ?: return null
        val isWrapped = line.startsWith("[") && line.endsWith("]")
        return line.substring(startIndex = 1, endIndex = line.length - 1).takeIf { isWrapped }
    }

    fun key(name: String): String? {
        EditInput.requireWritable(name)
        val isPlainWritten = lines(yamlScalar(name)) == listOf(name)
        val isFlowSafe = name.none { it in FLOW_INDICATORS } && FLOW_KEY_LEADS.none { name.startsWith(it) }
        val isPlain = isPlainWritten && isFlowSafe && QuotedText.isShownAsIs(name)
        val written = name.takeIf { isPlain } ?: QuotedText.of(name)
        return written.takeIf { it.length <= MAXIMUM_KEY_LENGTH }
    }

    private fun lines(value: YamlNode): List<String> {
        val emitted = yaml.emitValue(value, version, takenAnchors)
        val document = emitted.takeIf { !options.isDocumentStartExplicit } ?: withoutMarker(emitted)
        val body = document.removeSuffix("\n").removeSuffix("\r")
        return body.lines()
    }

    private fun withoutMarker(emitted: String): String {
        val afterMarker = emitted.removePrefix(DOCUMENT_START).removePrefix(" ")
        return afterMarker.removePrefix("\r\n").removePrefix("\n")
    }

    private fun isBlockScalar(value: YamlNode, lines: List<String>): Boolean =
        value is YamlNode.Scalar && lines.size > 1

    private fun isInline(value: YamlNode, lines: List<String>): Boolean {
        val first = lines.first()
        val isFlowText = first.startsWith("[") || first.startsWith("{")
        return lines.size == 1 && (value is YamlNode.Scalar || isFlowText)
    }

    private fun shifted(lines: List<String>, column: Int): String {
        val pad = " ".repeat(column)
        return lines.joinToString("") { line -> lineBreak + pad + line }
    }

    internal data class Rendering(val head: String, val tail: String)

    private companion object {
        const val ITEM_OFFSET: Int = 2
        const val MAXIMUM_KEY_LENGTH: Int = 1024
        const val DOCUMENT_START: String = "---"
        const val FLOW_INDICATORS: String = ",[]{}"
        const val FLOW_KEY_LEADS: String = "?:"
    }
}
