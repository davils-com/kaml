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

package com.davils.kaml.core.parser

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

internal class QuotedScan(private val state: ParseState, private val shape: Shape) {
    private val cursor = state.cursor
    private var start: Int = 0
    private var stoppedAt: Int = 0
    private var isClosed: Boolean = false

    fun endFrom(from: Int): Int {
        start = from
        stoppedAt = from + 1
        var index = from + 1
        while (index >= 0 && !isClosed) {
            index = step(index)
        }
        return index.takeIf { it >= 0 } ?: stoppedAt
    }

    private fun step(index: Int): Int {
        stoppedAt = index
        val character = state.charAt(index)
        if (character == END) {
            return stop(
                index,
                YamlDiagnosticCode.UnterminatedScalar,
                "A quoted scalar is not closed",
            )
        }
        val next = index + 1
        return quoteStep(index, character) ?: escapeStep(index, character) ?: breakStep(index, character) ?: next
    }

    private fun quoteStep(index: Int, character: Char): Int? {
        if (character != shape.quote) return null
        val isEscapedQuote = shape.quote == SINGLE_QUOTE && state.charAt(index + 1) == SINGLE_QUOTE
        return (index + 2).takeIf { isEscapedQuote } ?: close(index)
    }

    private fun close(index: Int): Int {
        isClosed = true
        stoppedAt = index + 1
        return index + 1
    }

    private fun escapeStep(index: Int, character: Char): Int? {
        val isEscape = character == BACKSLASH && shape.quote == DOUBLE_QUOTE
        return index.takeIf { isEscape }?.let(::escape)
    }

    private fun breakStep(index: Int, character: Char): Int? = index.takeIf { character.isBreak() }?.let(::nextLine)

    private fun stop(index: Int, code: YamlDiagnosticCode, message: String): Int {
        stoppedAt = index
        state.report(code, state.span(start = start, end = index), message)
        return -1
    }

    private fun nextLine(index: Int): Int {
        if (!shape.context.isMultiLine) {
            return stop(index, YamlDiagnosticCode.UnterminatedScalar, "A quoted key ends on its line")
        }
        val lineStart = cursor.nextLineStart(index)
        val isMarker = cursor.isDocumentMarkerAt(lineStart)
        if (!isMarker) reportIndentation(lineStart)
        val marker = YamlDiagnosticCode.DocumentMarkerInScalar
        return lineStart.takeIf { !isMarker } ?: stop(lineStart, marker, "A document marker cannot be inside a scalar")
    }

    private fun reportIndentation(lineStart: Int) {
        var spaces = lineStart
        while (state.charAt(spaces) == SPACE) {
            spaces++
        }
        var content = spaces
        while (state.charAt(content).isWhite()) {
            content++
        }
        val isBlank = state.charAt(content).isBreak()
        if (!isBlank && spaces - lineStart < shape.indent) {
            val range = state.span(start = lineStart, end = content)
            state.report(
                YamlDiagnosticCode.BadIndentation,
                range,
                "This line of a quoted scalar is not indented enough",
            )
        }
    }

    private fun escape(index: Int): Int {
        val code = state.charAt(index + 1)
        val width = HEX_WIDTHS[code]
        val isValid = code in SIMPLE_ESCAPES || code in QUOTE_ESCAPES || code == TAB || code.isBreak() || width != null
        if (!isValid) {
            val shown = SafeText.quoted("$BACKSLASH$code")
            val range = state.span(start = index, end = index + 2)
            state.report(YamlDiagnosticCode.InvalidEscape, range, "$shown is no escape sequence")
        }
        if (code.isBreak() || code == END) return index + 1
        return hexEnd(index, code, width ?: 0)
    }

    private fun hexEnd(index: Int, code: Char, width: Int): Int {
        val digits = hexDigitsFrom(from = index + 2, most = width)
        val end = index + 2 + digits
        val isTruncated = digits < width
        if (isTruncated) {
            state.report(
                YamlDiagnosticCode.InvalidEscape,
                state.span(start = index, end = end),
                "\\$code needs $width hex digits",
            )
        }
        if (width == 0 || isTruncated) return end
        return codePointEnd(index, code, end)
    }

    private fun hexDigitsFrom(from: Int, most: Int): Int {
        var count = 0
        while (count < most && state.charAt(from + count).isHexDigit()) {
            count++
        }
        return count
    }

    private fun codePointEnd(index: Int, code: Char, end: Int): Int {
        val value = hexValue(from = index + 2, to = end)
        val isPair = code == SHORT_UNICODE && value in HIGH_SURROGATES && isLowSurrogateEscapeAt(end)
        val isScalarValue = value <= LAST_CODE_POINT && value !in SURROGATES
        if (!isPair && !isScalarValue) {
            val shown = SafeText.quoted(state.text.substring(startIndex = index, endIndex = end))
            state.report(
                YamlDiagnosticCode.InvalidEscape,
                state.span(start = index, end = end),
                "$shown is a surrogate or lies beyond U+10FFFF, so it is no Unicode character",
            )
        }
        return (end + SHORT_UNICODE_LENGTH).takeIf { isPair } ?: end
    }

    private fun isLowSurrogateEscapeAt(index: Int): Boolean {
        val digits = index + SHORT_UNICODE_LENGTH
        val isEscape = state.charAt(index) == BACKSLASH && state.charAt(index + 1) == SHORT_UNICODE
        val hasDigits = isEscape && hexDigitsFrom(from = index + 2, most = UNICODE_DIGITS) == UNICODE_DIGITS
        return hasDigits && hexValue(from = index + 2, to = digits) in LOW_SURROGATES
    }

    private fun hexValue(from: Int, to: Int): Long {
        val digits = state.text.substring(startIndex = from, endIndex = to)
        return digits.toLong(HEX_RADIX)
    }

    data class Shape(val quote: Char, val indent: Int, val context: FlowContext)

    private companion object {
        const val SINGLE_QUOTE: Char = '\''
        const val DOUBLE_QUOTE: Char = '"'
        const val BACKSLASH: Char = '\\'
        const val SIMPLE_ESCAPES: String = "0abtnvfre /N_LP"
        const val QUOTE_ESCAPES: String = "\"\\"
        const val SHORT_UNICODE: Char = 'u'
        const val SHORT_UNICODE_LENGTH: Int = 6
        const val UNICODE_DIGITS: Int = 4
        const val HEX_RADIX: Int = 16
        const val LAST_CODE_POINT: Long = 0x10FFFF
        val HIGH_SURROGATES: LongRange = 0xD800L..0xDBFFL
        val LOW_SURROGATES: LongRange = 0xDC00L..0xDFFFL
        val SURROGATES: LongRange = 0xD800L..0xDFFFL
        val HEX_WIDTHS: Map<Char, Int> = mapOf('x' to 2, SHORT_UNICODE to UNICODE_DIGITS, 'U' to 8)
    }
}
