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

package com.davils.kaml.core.compose

internal object EscapeDecoder {
    private const val HEX_RADIX: Int = 16
    private const val SUPPLEMENTARY_START: Int = 0x10000
    private const val HIGH_SURROGATE_START: Int = 0xD800
    private const val LOW_SURROGATE_START: Int = 0xDC00
    private const val SURROGATE_BITS: Int = 10
    private const val LOW_SURROGATE_MASK: Int = 0x3FF
    private const val LARGEST_CODE_POINT: Int = 0x10FFFF
    private const val REPLACEMENT: Int = 0xFFFD
    private const val PAIRED_ESCAPE_LENGTH: Int = 6
    private const val UNICODE_ESCAPE: String = "\\u"
    private val HIGH_SURROGATES: LongRange = 0xD800L..0xDBFFL
    private val LOW_SURROGATES: LongRange = 0xDC00L..0xDFFFL
    private val SURROGATES: LongRange = 0xD800L..0xDFFFL
    private val NONCHARACTERS: LongRange = 0xFFFEL..0xFFFFL

    private val simple: Map<Char, Char> = mapOf(
        '0' to '\u0000',
        'a' to '\u0007',
        'b' to '\b',
        't' to '\t',
        '\t' to '\t',
        'n' to '\n',
        'v' to '\u000B',
        'f' to '\u000C',
        'r' to '\r',
        'e' to '\u001B',
        ' ' to ' ',
        '"' to '"',
        '/' to '/',
        '\\' to '\\',
        'N' to '\u0085',
        '_' to ' ',
        'L' to '\u2028',
        'P' to '\u2029',
    )

    private val widths: Map<Char, Int> = mapOf('x' to 2, 'u' to 4, 'U' to 8)

    fun decode(text: String, backslash: Int, target: StringBuilder): Int {
        val code = text[backslash + 1]
        val replacement = simple[code]
        if (replacement != null) {
            target.append(replacement)
            return backslash + 2
        }
        return hex(text, backslash, target)
    }

    private fun hex(text: String, backslash: Int, target: StringBuilder): Int {
        val code = text[backslash + 1]
        val end = minOf(a = backslash + 2 + (widths[code] ?: 0), b = text.length)
        val value = valueAt(text = text, from = backslash + 2, to = end) ?: code.code.toLong()
        val low = lowSurrogateAt(text, end).takeIf { code == 'u' && value in HIGH_SURROGATES }
        if (low != null) {
            target.append(value.toInt().toChar())
            target.append(low.toInt().toChar())
            return end + PAIRED_ESCAPE_LENGTH
        }
        appendCodePoint(target, value)
        return end
    }

    private fun lowSurrogateAt(text: String, index: Int): Long? {
        val isEscape = text.startsWith(UNICODE_ESCAPE, index)
        val value = valueAt(text = text, from = index + 2, to = index + PAIRED_ESCAPE_LENGTH)
        return value.takeIf { isEscape && value in LOW_SURROGATES }
    }

    private fun valueAt(text: String, from: Int, to: Int): Long? {
        if (from >= to || to > text.length) return null
        val digits = text.substring(startIndex = from, endIndex = to)
        return digits.toLongOrNull(HEX_RADIX)?.takeIf { digits.all(::isHexDigit) }
    }

    private fun isHexDigit(character: Char): Boolean =
        character in '0'..'9' || character in 'a'..'f' || character in 'A'..'F'

    private fun appendCodePoint(target: StringBuilder, codePoint: Long) {
        val isScalarValue = codePoint <= LARGEST_CODE_POINT && codePoint !in SURROGATES && codePoint !in NONCHARACTERS
        val safe = codePoint.toInt().takeIf { isScalarValue } ?: REPLACEMENT
        if (safe < SUPPLEMENTARY_START) {
            target.append(safe.toChar())
            return
        }
        val offset = safe - SUPPLEMENTARY_START
        target.append((HIGH_SURROGATE_START + (offset shr SURROGATE_BITS)).toChar())
        target.append((LOW_SURROGATE_START + (offset and LOW_SURROGATE_MASK)).toChar())
    }
}
