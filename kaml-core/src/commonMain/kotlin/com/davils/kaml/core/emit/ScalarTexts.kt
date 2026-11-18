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

package com.davils.kaml.core.emit

internal object ScalarTexts {
    private const val INDICATORS: String = "-?:,[]{}#&*!|>'\"%@`"
    private const val FLOW_INDICATORS: String = ",[]{}"
    private const val LEADS: String = "-?:"
    private const val FLOW_KEY_LEADS: String = "?:"
    private const val BLANKS: String = " \t"
    private const val LINE_FEED: Char = '\n'
    private const val HEX_RADIX: Int = 16
    private const val LAST_BYTE: Int = 0xFF
    private const val LAST_BASIC: Int = 0xFFFF
    private const val BYTE_DIGITS: Int = 2
    private const val BASIC_DIGITS: Int = 4
    private const val WIDE_DIGITS: Int = 8
    private const val BYTE_ESCAPE: String = "\\x"
    private const val BASIC_ESCAPE: String = "\\u"
    private const val WIDE_ESCAPE: String = "\\U"
    private const val STRIP: String = "-"
    private const val KEEP: String = "+"
    private const val LITERAL: String = "|"

    private val bodyBreakers: List<String> = listOf(": ", " #", ":\t", "\t#")

    private val escapes: Map<Int, String> = mapOf(
        '\\'.code to "\\\\",
        '"'.code to "\\\"",
        '\n'.code to "\\n",
        '\t'.code to "\\t",
        '\r'.code to "\\r",
        '\u0000'.code to "\\0",
        '\u0007'.code to "\\a",
        '\b'.code to "\\b",
        '\u000B'.code to "\\v",
        '\u000C'.code to "\\f",
        '\u001B'.code to "\\e",
        '\u0085'.code to "\\N",
        '\u2028'.code to "\\L",
        '\u2029'.code to "\\P",
    )

    fun isPlainSafe(value: String, context: ScalarContext): Boolean {
        val isTrimmed = value.isNotEmpty() && value.first() !in BLANKS && value.last() !in BLANKS
        val isShaped = isTrimmed && isVisibleLine(value)
        val isFlowFit = !context.isInFlow || isFlowSafe(value)
        return isShaped && isFlowFit && hasPlainStart(value) && hasPlainBody(value)
    }

    fun isSingleQuotable(value: String): Boolean = isVisibleLine(value)

    fun isBlockable(value: String): Boolean {
        val hasContent = value.any { it != LINE_FEED }
        return hasContent && UnsafeCharacters.firstUnsafeIndex(value) == UnsafeCharacters.NONE
    }

    fun singleQuoted(value: String, out: OutputBuffer) {
        out.append('\'')
        value.forEach { character ->
            if (character == '\'') out.append(character)
            out.append(character)
        }
        out.append('\'')
    }

    fun doubleQuoted(value: String, out: OutputBuffer) {
        out.append('"')
        var index = 0
        while (index < value.length) {
            val codePoint = UnsafeCharacters.codePointAt(value, index)
            val width = UnsafeCharacters.widthOf(codePoint)
            val escape = escapeOf(codePoint)
            if (escape != null) out.append(escape)
            if (escape == null) out.append(value, start = index, end = index + width)
            index += width
        }
        out.append('"')
    }

    fun literalHeader(value: String, width: Int): String {
        val trailing = value.length - value.trimEnd(LINE_FEED).length
        val firstContent = value.indexOfFirst { it != LINE_FEED }
        val isIndented = value.startsWith(" ", startIndex = firstContent)
        val indicator = "$width".takeIf { isIndented }.orEmpty()
        return LITERAL + indicator + chompingOf(trailing)
    }

    fun literalLines(value: String, indent: Int, lineBreak: String, out: OutputBuffer) {
        val end = value.removeSuffix("$LINE_FEED").length
        var start = 0
        while (start <= end) {
            val lineEnd = value.indexOf(LINE_FEED, startIndex = start).takeIf { it in start until end } ?: end
            if (lineEnd > start) out.spaces(indent)
            out.append(value, start = start, end = lineEnd)
            out.append(lineBreak)
            start = lineEnd + 1
        }
    }

    private fun chompingOf(trailing: Int): String {
        if (trailing == 0) return STRIP
        return KEEP.takeIf { trailing > 1 }.orEmpty()
    }

    private fun isFlowSafe(value: String): Boolean {
        val isKeyLead = FLOW_KEY_LEADS.any { lead -> value.startsWith(lead) }
        return !isKeyLead && value.none { it in FLOW_INDICATORS }
    }

    private fun isVisibleLine(value: String): Boolean =
        LINE_FEED !in value && UnsafeCharacters.firstUnsafeIndex(value) == UnsafeCharacters.NONE

    private fun hasPlainStart(value: String): Boolean {
        val next = value.getOrNull(1)
        val isSafeNext = next != null && next !in BLANKS
        val isMarker = value.startsWith("---") || value.startsWith("...")
        val isSafeLead = value.first() in LEADS && isSafeNext
        val isIndicatorFree = value.first() !in INDICATORS || isSafeLead
        return isIndicatorFree && !isMarker
    }

    private fun hasPlainBody(value: String): Boolean = bodyBreakers.none(value::contains) && !value.endsWith(":")

    private fun escapeOf(codePoint: Int): String? =
        escapes[codePoint] ?: codePoint.takeIf(UnsafeCharacters::isUnsafe)?.let(::hexEscape)

    private fun hexEscape(codePoint: Int): String {
        val isByte = codePoint <= LAST_BYTE
        val isBasic = codePoint <= LAST_BASIC
        val prefix = BYTE_ESCAPE.takeIf { isByte } ?: BASIC_ESCAPE.takeIf { isBasic } ?: WIDE_ESCAPE
        val width = BYTE_DIGITS.takeIf { isByte } ?: BASIC_DIGITS.takeIf { isBasic } ?: WIDE_DIGITS
        val digits = codePoint.toString(HEX_RADIX).uppercase()
        return prefix + digits.padStart(length = width, padChar = '0')
    }
}
